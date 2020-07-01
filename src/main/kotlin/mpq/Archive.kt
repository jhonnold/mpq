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

    init {
        val magic = this.readFromChannel(0, 4)
        this.userData = if (magic.compareTo(UserData.headerId) == 0) readUserData() else null
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
