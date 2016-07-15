package compiler;

import java.io.*;

class IOUtils {

    private static final int EOF = -1;

    /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static String slurp(final InputStream is, final int bufferSize) throws IOException {
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        final Reader in = new InputStreamReader(is, "UTF-8");
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

    public static String slurp(final InputStream is) throws IOException {
        return slurp(is, 4096);
    }

    public static void writeToFile(File file, String data) throws IOException {
        FileOutputStream os = new FileOutputStream(file);
        os.write(data.getBytes());
        os.close();
    }

    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
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
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since 1.3
     */
    public static long copy(InputStream input, OutputStream output)
            throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }
}
