package mpq

import org.junit.Test
import java.nio.ByteBuffer
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ArchiveTest {
    @Test
    fun userData() {
        val projectDirPath = Paths.get("").toAbsolutePath()
        val resourcePath = Paths.get(projectDirPath.toString(), "/src/test/resources/archive")

        Archive(resourcePath).use {
            val userData = it.userData
            assertNotNull(userData)
            assertEquals(
                    ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B)),
                    userData.magic)
            assertEquals(512, userData.userDataSize)
            assertEquals(1024, userData.headerOffset)
            assertEquals(115, userData.userDataHeaderSize)
        }
    }

    @Test
    fun header() {
        val projectDirPath = Paths.get("").toAbsolutePath()
        val resourcePath = Paths.get(projectDirPath.toString(), "/src/test/resources/archive")

        Archive(resourcePath).use {
            val header = it.header

            assertEquals(
                    ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1A)),
                    header.magic)
            assertEquals(208, header.headerSize)
            assertEquals(68730, header.archiveSize)
            assertEquals(3, header.formatVersion)
            assertEquals(5, header.sectorSizeShift)
            assertEquals(67946, header.hashTableOffset)
            assertEquals(68458, header.blockTableOffset)
            assertEquals(32, header.hashTableEntries)
            assertEquals(17, header.blockTableEntries)
        }
    }

    @Test
    fun hashTable() {
        val projectDirPath = Paths.get("").toAbsolutePath()
        val resourcePath = Paths.get(projectDirPath.toString(), "/src/test/resources/archive")

        Archive(resourcePath).use {
            val table = it.hashTable

            assertEquals(32, table.size)

            val entry0: HashEntry = table[0]
            assertEquals(0xD609C5AE.toInt(), entry0.fileHashA)
            assertEquals(0xCAA8D159.toInt(), entry0.fileHashB)
            assertEquals(0, entry0.language)
            assertEquals(0, entry0.platform)
            assertEquals(1, entry0.fileBlock)

            val entry31: HashEntry = table[31]
            assertEquals(0x31952289, entry31.fileHashA)
            assertEquals(0x6A5FFAA3, entry31.fileHashB)
            assertEquals(0, entry31.language)
            assertEquals(0, entry31.platform)
            assertEquals(7, entry31.fileBlock)
        }
    }
}