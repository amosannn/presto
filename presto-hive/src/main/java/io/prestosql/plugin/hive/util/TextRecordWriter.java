/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.plugin.hive.util;

import io.prestosql.plugin.hive.RecordFileWriter.ExtendedRecordWriter;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static org.apache.hadoop.hive.ql.exec.Utilities.createCompressedStream;

public class TextRecordWriter
        implements ExtendedRecordWriter
{
    private final FSDataOutputStream output;
    private final OutputStream compressedOutput;
    private final int rowSeparator;

    public TextRecordWriter(Path path, JobConf jobConf, Properties properties, boolean isCompressed)
            throws IOException
    {
        String rowSeparatorString = properties.getProperty(serdeConstants.LINE_DELIM, "\n");
        // same logic as HiveIgnoreKeyTextOutputFormat
        int rowSeparatorByte;
        try {
            rowSeparatorByte = Byte.parseByte(rowSeparatorString);
        }
        catch (NumberFormatException e) {
            rowSeparatorByte = rowSeparatorString.charAt(0);
        }
        rowSeparator = rowSeparatorByte;
        output = path.getFileSystem(jobConf).create(path, Reporter.NULL);
        compressedOutput = createCompressedStream(jobConf, output, isCompressed);
    }

    @Override
    public long getWrittenBytes()
    {
        return output.getPos();
    }

    @Override
    public void write(Writable writable)
            throws IOException
    {
        BinaryComparable binary = (BinaryComparable) writable;
        compressedOutput.write(binary.getBytes(), 0, binary.getLength());
        compressedOutput.write(rowSeparator);
    }

    @Override
    public void close(boolean abort)
            throws IOException
    {
        compressedOutput.close();
    }
}
