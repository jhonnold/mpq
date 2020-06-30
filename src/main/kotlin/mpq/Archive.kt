package mpq

import java.io.RandomAccessFile
import java.lang.UnsupportedOperationException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class Archive(pathname: String) {
    private val file = RandomAccessFile(pathname, "r")
    val header: Header

    init {
        this.header = Header(file)
    }

    fun close() {
       file.close();
    }
}

class Header(file: RandomAccessFile) {
    private val magic  = ByteArray(4)
    private val headerSize = ByteArray(4)
    private val archiveSize = ByteArray(4)
    private val formatVersion = ByteArray(2)
    private val sectorSizeShift = ByteArray(2)
    private val hashTableOffset = ByteArray(4)
    private val blockTableOffset = ByteArray(4)
    private val hashTableEntries = ByteArray(4)
    private val blockTableEntries = ByteArray(4)

    var offset: Int = 0
    var userData: UserData? = null

    init {
        val magic = ByteArray(4)
        file.read(magic)
        file.seek(0)

        when {
            magic.contentEquals(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B)) -> {
                this.userData = UserData(file)

                this.readHeader(file, this.userData!!.getHeaderOffset().toLong())
            }
            magic.contentEquals(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1A)) -> {
                this.readHeader(file)
            }
            else -> {
                throw UnsupportedOperationException("Unknown header")
            }
        }
    }

    fun getHeaderSize() = getInt(this.headerSize)
    fun getArchiveSize() = getInt(this.archiveSize)
    fun getFormatVersion() = getShort(this.formatVersion)
    fun getSectorSizeShift() = getShort(this.sectorSizeShift)
    fun getHashTableOffset() = getInt(this.hashTableOffset)
    fun getBlockTableOffset() = getInt(this.blockTableOffset)
    fun getHashTableEntries() = getInt(this.hashTableEntries)
    fun getBlockTableEntries() = getInt(this.blockTableEntries)

    private fun readHeader(file: RandomAccessFile, offset: Long = 0) {
        file.seek(offset)
        file.read(this.magic)
        file.read(this.headerSize)
        file.read(this.archiveSize)
        file.read(this.formatVersion)
        file.read(this.sectorSizeShift)
        file.read(this.hashTableOffset)
        file.read(this.blockTableOffset)
        file.read(this.hashTableEntries)
        file.read(this.blockTableEntries)

        this.offset = offset.toInt()
    }
}

class UserData(file: RandomAccessFile) {
    private val magic = ByteArray(4)
    private val userDataSize= ByteArray(4)
    private val headerOffset= ByteArray(4)
    private val userDataHeaderSize = ByteArray(4)
    private val content: ByteArray

    init {
        file.read(this.magic)
        file.read(this.userDataSize)
        file.read(this.headerOffset)
        file.read(this.userDataHeaderSize)

        content = ByteArray(this.getUserDataHeaderSize())
        file.read(this.content)
    }

    fun getUserDataSize() = getInt(this.userDataSize)
    fun getHeaderOffset() = getInt(this.headerOffset)
    fun getUserDataHeaderSize() = getInt(this.userDataHeaderSize)
    fun getContent() = this.content.toString(Charset.defaultCharset())
}

class HashTable(file: RandomAccessFile) {

}

fun getShort(b: ByteArray) = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short
fun getInt(b: ByteArray) = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
