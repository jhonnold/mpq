package mpq

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.file.Path

class Archive(path: Path) : AutoCloseable {
    private val stream = FileInputStream(path.toFile())
    private val channel: SeekableByteChannel = stream.channel
    private val encryptionTable: LongArray

    val userData: UserData?
    val header: Header
    val hashTable: List<HashEntry>

    init {
        this.encryptionTable = this.prepEncryptionTable()

        val magic = this.readFromChannel(0, 4)
        this.userData = if (magic == UserData.headerId) this.readUserData() else null
        this.header = this.readHeader()
        this.hashTable = this.readHashTable()
    }

    private fun prepEncryptionTable(): LongArray {
        val table = LongArray(0x500)
        var seed: Long = 0x0010_0001

        (0..0xFF).forEach { i ->
            var idx = i
            (0..0x4).forEach { _ ->
                seed = (seed * 125 + 3) % 0x2AAAAB
                val t1 = (seed and 0xFFFF) shl 0x10

                seed = (seed * 125 + 3) % 0x2AAAAB
                val t2 = seed and 0xFFFF

                table[idx] = t1 or t2
                idx += 0x100
            }
        }

        return table
    }

    private fun hash(data: String, type: HashType): Int {
        var seed1: Long = 0x7FED_7FED
        var seed2: Long = 0xEEEE_EEEE

        data.forEach { c ->
            val ch = c.toUpperCase()
            val value = this.encryptionTable[ch.toInt() + type.offset]
            seed1 = value xor ((seed1 + seed2) and 0xFFFF_FFFF)
            seed2 = (ch.toInt() + seed1 + seed2 + (seed2 shl 5) + 3) and 0xFFFF_FFFF
        }

        return seed1.toInt()
    }

    private fun decrypt(buffer: ByteBuffer, len: Int, key: Int): ByteBuffer {
        val result = ByteBuffer.wrap(ByteArray(len))
        var seed1: Long = key.toLong() and 0xFFFF_FFFF
        var seed2: Long = 0xEEEE_EEEE

        (0 until len step 4).forEach {
            seed2 = (seed2 + this.encryptionTable[0x400 + (seed1 and 0xFF).toInt()]) and 0xFFFF_FFFF

            val value = buffer.getInt(it) xor (seed1 + seed2).toInt()

            seed1 = (((seed1.inv() shl 0x15) + 0x1111_1111) or (seed1 shr 0x0B)) and 0xFFFF_FFFF
            seed2 = (value + seed2 + (seed2 shl 5) + 3) and 0xFFFF_FFFF

            result.putInt(it, value)
        }

        return result.rewind()
    }

    private fun readUserData(): UserData {
        val buffer = this.readFromChannel(0, 16)

        val magic = ByteArray(4)
        buffer.get(magic)

        val userDataSize = buffer.int
        val headerOffset = buffer.int
        val userDataHeaderSize = buffer.int

        val content = this.readFromChannel(16, userDataHeaderSize)

        return UserData(
                ByteBuffer.wrap(magic),
                userDataSize,
                headerOffset,
                userDataHeaderSize,
                content
        )
    }

    private fun readHeader(): Header {
        val headerOffset = userData?.headerOffset?.toLong() ?: 0
        val buffer = this.readFromChannel(headerOffset, 32)

        val magic = ByteArray(4)
        buffer.get(magic)

        val headerSize = buffer.int
        val archiveSize = buffer.int
        val formatVersion = buffer.short
        val sectorSizeShift = buffer.short
        val hashTableOffset = buffer.int
        val blockTableOffset = buffer.int
        val hashTableEntries = buffer.int
        val blockTableEntries = buffer.int

        return Header(
                userData?.headerOffset ?: 0,
                ByteBuffer.wrap(magic),
                headerSize,
                archiveSize,
                formatVersion.toInt(),
                sectorSizeShift.toInt(),
                hashTableOffset,
                blockTableOffset,
                hashTableEntries,
                blockTableEntries
        )
    }

    private fun readHashTable(): List<HashEntry> {
        val key = this.hash("(hash table)", HashType.TABLE)
        val size = HashEntry.SIZE * header.hashTableEntries
         
        val encrypted = this.readFromChannel((header.offset + header.hashTableOffset).toLong(), size)
        val decrypted = this.decrypt(encrypted, size, key)

        return (0 until header.hashTableEntries).map {
            val fileHashA = decrypted.int
            val fileHashB = decrypted.int
            val language = decrypted.short
            val platform = decrypted.short
            val fileBlock = decrypted.int

            HashEntry(fileHashA, fileHashB, language, platform, fileBlock)
        }
    }

    private fun readFromChannel(position: Long, size: Int): ByteBuffer {
        val originalPosition = this.channel.position()
        this.channel.position(position)

        val buffer = ByteBuffer.wrap(ByteArray(size))

        this.channel.read(buffer)
        this.channel.position(originalPosition)

        return buffer.order(ByteOrder.LITTLE_ENDIAN).rewind()
    }

    override fun close() {
        stream.close()
    }
}

enum class HashType(val offset: Int) {
    TABLE_OFFSET(0 shl 8),
    HASH_A(1 shl 8),
    HASH_B(2 shl 8),
    TABLE(3 shl 8)
}

class UserData(val magic: ByteBuffer, val userDataSize: Int, val headerOffset: Int, val userDataHeaderSize: Int, val content: ByteBuffer) {
    companion object {
        val headerId: ByteBuffer = ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B))
    }
}

class Header(val offset: Int, val magic: ByteBuffer, val headerSize: Int, val archiveSize: Int, val formatVersion: Int,
             val sectorSizeShift: Int, val hashTableOffset: Int, val blockTableOffset: Int, val hashTableEntries: Int, val blockTableEntries: Int)

class HashEntry(val fileHashA: Int, val fileHashB: Int, val language: Short, val platform: Short, val fileBlock: Int) {
    companion object {
        const val SIZE = 16
    }

    override fun toString(): String {
        return "%08x %08x %04x %04x %08x".format(fileHashA, fileHashB, language, platform, fileBlock)
    }
}

fun printBuffer(buffer: ByteBuffer) {
    val arr = buffer.array()
    println(arr.map { "%02x".format(it) })
}
