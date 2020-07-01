package mpq

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.file.Path

class Archive(path: Path) : AutoCloseable {
    private val stream = FileInputStream(path.toFile())
    private val channel: SeekableByteChannel = stream.channel

    val userData: UserData?
    val header: Header

    init {
        val magic = this.readFromChannel(0, 4)
        this.userData = if (magic == UserData.headerId) this.readUserData() else null

        val headerOffset = userData?.headerOffset?.toLong() ?: 0
        this.header = this.readHeader(headerOffset)
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

    private fun readHeader(position: Long): Header {
        val buffer =  this.readFromChannel(position, 32)

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

class UserData(val magic: ByteBuffer, val userDataSize: Int, val headerOffset: Int, val userDataHeaderSize: Int, val content: ByteBuffer) {
    companion object {
        val headerId: ByteBuffer = ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B))
    }
}

class Header(val magic: ByteBuffer, val headerSize: Int, val archiveSize: Int, val formatVersion: Int, val sectorSizeShift: Int,
             val hashTableOffset: Int, val blockTableOffset: Int, val hashTableEntries: Int, val blockTableEntries: Int)
