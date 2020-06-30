package mpq

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ArchiveTest {
    @Test fun init() {
        val pathname = this.javaClass.getResource("/archive")!!.file;
        val archive = Archive(pathname);

        assertNotNull(archive.header.userData)
        assertEquals(512, archive.header.userData!!.getUserDataSize())
        assertEquals(1024, archive.header.userData!!.getHeaderOffset())
        assertEquals(115, archive.header.userData!!.getUserDataHeaderSize())

        println(archive.header.userData!!.getContent())

        assertEquals(208, archive.header.getHeaderSize())
        assertEquals(68730, archive.header.getArchiveSize())
        assertEquals(3, archive.header.getFormatVersion())
        assertEquals(5, archive.header.getSectorSizeShift())
        assertEquals(67946, archive.header.getHashTableOffset())
        assertEquals(68458, archive.header.getBlockTableOffset())
        assertEquals(32, archive.header.getHashTableEntries())
        assertEquals(17, archive.header.getBlockTableEntries())
        assertEquals(1024, archive.header.offset)
    }
}