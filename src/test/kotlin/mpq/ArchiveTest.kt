package mpq

import org.junit.Test
import java.nio.ByteBuffer
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArchiveTest {
    @Test
    fun init() {
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
}