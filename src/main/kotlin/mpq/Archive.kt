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
        this.userData = if (magic.compareTo(UserData.headerId) == 0) this.readUserData() else null

        val headerOffset = userData?.headerOffset?.toLong() ?: 0
        this.header = this.readHeader(headerOffset)
    }

    private fun readUserData(): UserData {
        val magic = this.readFromChannel(0, 4)
        val userDataSize = this.readFromChannel(4, 4).getInt(0)
        val headerOffset = this.readFromChannel(8, 4).getInt(0)
        val userDataHeaderSize = this.readFromChannel(12, 4).getInt(0)
        val content = this.readFromChannel(16, userDataHeaderSize)

        return UserData(
                magic,
                userDataSize,
                headerOffset,
                userDataHeaderSize,
                content
        )
    }

    private fun readHeader(position: Long): Header {
        val magic = this.readFromChannel(position, 4)
        val headerSize = this.readFromChannel(position + 4, 4).getInt(0)
        val archiveSize = this.readFromChannel(position + 8, 4).getInt(0)
        val formatVersion = this.readFromChannel(position + 12, 2).getShort(0)
        val sectorSizeShift = this.readFromChannel(position + 14, 2).getShort(0)
        val hashTableOffset = this.readFromChannel(position + 16, 4).getInt(0)
        val blockTableOffset = this.readFromChannel(position + 20, 4).getInt(0)
        val hashTableEntries = this.readFromChannel(position + 24, 4).getInt(0)
        val blockTableEntries = this.readFromChannel(position + 28, 4).getInt(0)

        return Header(
                magic,
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

        return buffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    override fun close() {
        stream.close()
    }
}

class UserData(val magic: ByteBuffer, val userDataSize: Int, val headerOffset: Int, val userDataHeaderSize: Int, val content: ByteBuffer) {
    companion object {
        val headerId: ByteBuffer = ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B)).position(4)
    }
}

class Header(val magic: ByteBuffer, val headerSize: Int, val archiveSize: Int, val formatVersion: Int, val sectorSizeShift: Int,
             val hashTableOffset: Int, val blockTableOffset: Int, val hashTableEntries: Int, val blockTableEntries: Int)
