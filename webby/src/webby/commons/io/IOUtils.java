package webby.commons.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class IOUtils {

    private static final int EOF = -1;

    /**
     * The default buffer size ({@value}) to use for {@link #copyLarge(java.io.InputStream,
     * java.io.OutputStream, byte[])}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        return ByteStreams.toByteArray(inputStream);
    }

    public static byte[] readBytes(File file) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return ByteStreams.toByteArray(stream);
        }
    }

    public static String readString(final InputStream is, final int bufferSize, Charset charset) throws IOException {
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        final Reader in = new InputStreamReader(is, charset);
        try {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } finally {
            in.close();
        }
        return out.toString();
    }

    public static String readString(final InputStream is) throws IOException {
        return readString(is, 4096, Charsets.UTF_8);
    }

    public static String readString(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        String contents = readString(stream);
        stream.close();
        return contents;
    }

    public static String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path), Charsets.UTF_8);
    }

    public static String readString(File file, Charset charset) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        String contents = readString(stream, 4096, charset);
        stream.close();
        return contents;
    }

    public static void writeToFile(Path path, String data) throws IOException {
        Files.write(path, data.getBytes(Charsets.UTF_8));
    }

    public static void writeToFile(Path path, byte[] data) throws IOException {
        Files.write(path, data);
    }

    public static void writeToFile(File file, String data) throws IOException {
        writeToFile(file, data.getBytes(Charsets.UTF_8));
    }

    public static void writeToFile(File file, byte[] data) throws IOException {
        FileOutputStream os = new FileOutputStream(file);
        os.write(data);
        os.close();
    }

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws java.io.IOException  if an I/O error occurs
     */
    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
         throws IOException {
        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws java.io.IOException  if an I/O error occurs
     * @since 1.3
     */
    public static long copy(InputStream input, OutputStream output)
         throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * Посчитать crc32 от стрима.
     */
    public static long crc32(final InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        CRC32 crc = new CRC32();
        crc.reset();
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            crc.update(buffer, 0, n);
        }
        return crc.getValue();
    }
}
