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
                    ByteBuffer.wrap(byteArrayOf('M'.toByte(), 'P'.toByte(), 'Q'.toByte(), 0x1B)).position(4),
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
}