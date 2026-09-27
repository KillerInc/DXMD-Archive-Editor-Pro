import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import options.BooleanOption;
import options.ByteOption;
import options.FloatOption;
import options.InventoryXOption;
import options.NumericOption;
import options.Option;
import options.ShortOption;

/** Reads current archive values for the normal editor controls. */
public final class FileAnalyzer {
    private FileAnalyzer() {}

    public static void analyze(File gameFile, ArrayList<Option> optionData) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(gameFile, "r")) {
            for (Option option : optionData) {
                if (option.getAddresses() == null || option.getAddresses().isEmpty())
                    throw new IOException("No archive address is configured for " + option.getOptionName() + ".");

                if (option instanceof ShortOption) {
                    int first = readU16LE(file, option.getAddresses().get(0));
                    boolean mixed = false;
                    for (int i = 1; i < option.getAddresses().size(); i++)
                        mixed |= readU16LE(file, option.getAddresses().get(i)) != first;
                    ((NumericOption) option).setCurrentFileValue(first);
                    option.setMixedCurrentValues(mixed);
                } else if (option instanceof InventoryXOption || option instanceof ByteOption) {
                    int first = readU8(file, option.getAddresses().get(0));
                    boolean mixed = false;
                    for (int i = 1; i < option.getAddresses().size(); i++)
                        mixed |= readU8(file, option.getAddresses().get(i)) != first;
                    ((NumericOption) option).setCurrentFileValue(first);
                    option.setMixedCurrentValues(mixed);
                } else if (option instanceof FloatOption) {
                    int firstBits = readI32LE(file, option.getAddresses().get(0));
                    boolean mixed = false;
                    for (int i = 1; i < option.getAddresses().size(); i++)
                        mixed |= readI32LE(file, option.getAddresses().get(i)) != firstBits;
                    ((FloatOption) option).setCurrentFileValue(Float.intBitsToFloat(firstBits));
                    option.setMixedCurrentValues(mixed);
                } else if (option instanceof BooleanOption booleanOption) {
                    boolean first = readLogicalBoolean(file, booleanOption, 0);
                    boolean mixed = false;
                    for (int i = 1; i < option.getAddresses().size(); i++)
                        mixed |= readLogicalBoolean(file, booleanOption, i) != first;
                    booleanOption.setCurrentFileValue(first);
                    option.setMixedCurrentValues(mixed);
                } else {
                    throw new IOException("Unsupported editor option type: " + option.getClass().getName());
                }
            }
        }
    }

    private static int readU8(RandomAccessFile file, long address) throws IOException {
        file.seek(address);
        return file.readUnsignedByte();
    }

    private static int readU16LE(RandomAccessFile file, long address) throws IOException {
        file.seek(address);
        int lo = file.readUnsignedByte();
        int hi = file.readUnsignedByte();
        return lo | (hi << 8);
    }

    private static int readI32LE(RandomAccessFile file, long address) throws IOException {
        file.seek(address);
        return file.readUnsignedByte()
                | (file.readUnsignedByte() << 8)
                | (file.readUnsignedByte() << 16)
                | (file.readUnsignedByte() << 24);
    }

    private static boolean readLogicalBoolean(RandomAccessFile file, BooleanOption option, int index) throws IOException {
        int raw = readU8(file, option.getAddresses().get(index));
        return raw != (option.getSpecificValueFalseVals(index) & 0xFF);
    }
}
