package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ByteOption extends NumericOption {
    public ByteOption(ArrayList<Long> addresses, String optionName, String optionDesc, int defaultValue) {
        maxValue = 255;
        this.addresses = addresses;
        this.optionName = optionName;
        this.optionDesc = optionDesc;
        optionValues = new ArrayList<Integer>();
        optionValues.add(0); optionValues.add(defaultValue); optionValues.add(0);
    }
    @Override public void makeChanges(RandomAccessFile f) throws IOException {
        for (Long address : addresses) { f.seek(address); f.writeByte(optionValues.get(2)); }
    }
}
