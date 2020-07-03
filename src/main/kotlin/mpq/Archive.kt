package mpq

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.Inflater
import kotlin.math.ceil

class Archive(path: Path) : AutoCloseable {
    private val stream = FileInputStream(path.toFile())
    private val channel: SeekableByteChannel = stream.channel
    private val encryptionTable: LongArray

    val userData: UserData?
    val header: Header
    val hashTable: List<HashEntry>
    val blockTable: List<BlockEntry>
    val files: List<String>

    init {
        this.encryptionTable = this.prepEncryptionTable()

        val magic = this.readFromChannel(0, 4)
        this.userData = if (magic == UserData.headerId) this.readUserData() else null
        this.header = this.readHeader()
        this.hashTable = this.readHashTable()
        this.blockTable = this.readBlockTable()

        val listFileContents = this.getFileContents("(listfile)")
        this.files = StandardCharsets.UTF_8.decode(listFileContents)
                .toString().trim().split("\r\n", "\r", "\n")
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

    private fun decompress(buffer: ByteBuffer, size: Int): ByteBuffer {
        val dest = ByteArray(size)
        val array = buffer.array()

        when (val compressionType = buffer.get()) {
            0x02.toByte() -> {
                val inflater = Inflater()
                inflater.setInput(array, 1, array.size - 1)
                inflater.inflate(dest)
                inflater.end()
            }
            0x10.toByte() -> BZip2CompressorInputStream(ByteArrayInputStream(array, 1, array.size - 1)).use { it.read(dest) }
            else -> throw Exception("Compression Type: %d is not supported".format(compressionType))
        }

        return ByteBuffer.wrap(dest)
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

    private fun readBlockTable(): List<BlockEntry> {
        val key = this.hash("(block table)", HashType.TABLE)
        val tableSize = BlockEntry.SIZE * header.blockTableEntries

        val encrypted = this.readFromChannel((header.offset + header.blockTableOffset).toLong(), tableSize)
        val decrypted = this.decrypt(encrypted, tableSize, key)

        return (0 until header.blockTableEntries).map {
            val offset = decrypted.int
            val archivedSize = decrypted.int
            val size = decrypted.int
            val flags = decrypted.int

            BlockEntry(offset, archivedSize, size, flags)
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

    fun getFileContents(filename: String): ByteBuffer {
        val hashA = this.hash(filename, HashType.HASH_A)
        val hashB = this.hash(filename, HashType.HASH_B)
        val hashEntry = this.hashTable.find { it.fileHashA == hashA && it.fileHashB == hashB }
                ?: throw Exception("File %s was not found in the Hash Table!".format(filename))

        val blockEntry = this.blockTable[hashEntry.fileBlock]
        if (blockEntry.size == 0) return ByteBuffer.allocate(0)

        if (blockEntry.flags and 0x8000_0000.toInt() == 0)
            throw Exception("File %S does not exist in block table!".format(filename))
        if (blockEntry.flags and 0x0001_0000 != 0)
            throw Exception("Encryption not supported")

        val fileData = this.readFromChannel((this.header.offset + blockEntry.offset).toLong(), blockEntry.archivedSize)

        if (blockEntry.flags and 0x0100_0000 != 0) {
            if (blockEntry.flags and 0x0000_0200 != 0 && blockEntry.size > blockEntry.archivedSize)
                return this.decompress(fileData, blockEntry.size)

            return fileData
        } else {
            val result = ByteBuffer.allocate(blockEntry.size)

            val sectorSize = 512 shl this.header.sectorSizeShift
            var sectors = ceil(blockEntry.size.toDouble() / sectorSize).toInt()

            var crc = false
            if (blockEntry.flags and 0x0400_0000 != 0) {
                crc = true
                sectors++
            }

            val positions = (0..sectors).map { fileData.int }
            val validSectors = positions.size - if (crc) 2 else 1

            (0 until validSectors).forEach {
                val archivedSectorSize = positions[it + 1] - positions[it]
                val sector = ByteArray(archivedSectorSize)

                fileData.get(sector)
                val sectorBuffer = ByteBuffer.wrap(sector)
                if (blockEntry.flags and 0x0000_0200 != 0) {
                    result.put(this.decompress(sectorBuffer, sectorSize.coerceAtMost(result.remaining())))
                } else
                    result.put(sectorBuffer)
            }

            return result.order(ByteOrder.LITTLE_ENDIAN).rewind()
        }
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

class BlockEntry(val offset: Int, val archivedSize: Int, val size: Int, val flags: Int) {
    companion object {
        const val SIZE = 16
    }

    override fun toString(): String {
        return "%08x %08x %08x %08x".format(offset, archivedSize, size, flags)
    }
}
