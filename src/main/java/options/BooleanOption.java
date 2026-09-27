package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/** Boolean archive control with address-specific false/true byte encodings. */
public class BooleanOption extends Option {
    // 0. current file value, 1. default value, 2. new value.
    private final ArrayList<Boolean> optionValues;
    private final ArrayList<Short> trueVals;
    private final ArrayList<Short> falseVals;

    public BooleanOption(ArrayList<Long> addresses, ArrayList<Short> falseVals, ArrayList<Short> trueVals,
                         String optionName, String optionDesc, boolean defaultValue) {
        if (addresses == null || addresses.isEmpty()) throw new IllegalArgumentException("Boolean option requires at least one address.");
        if (falseVals == null || trueVals == null || falseVals.size() != addresses.size() || trueVals.size() != addresses.size())
            throw new IllegalArgumentException("Boolean option address/value counts do not match for " + optionName + ".");
        this.addresses = addresses;
        this.optionName = optionName;
        this.optionDesc = optionDesc;
        this.falseVals = falseVals;
        this.trueVals = trueVals;
        optionValues = new ArrayList<>();
        optionValues.add(false);
        optionValues.add(defaultValue);
        optionValues.add(false);
    }

    @Override
    public void makeChanges(RandomAccessFile accessGameFile) throws IOException {
        for (int i = 0; i < addresses.size(); i++) {
            accessGameFile.seek(addresses.get(i));
            accessGameFile.writeByte(optionValues.get(2) ? trueVals.get(i) : falseVals.get(i));
        }
    }

    public void setCurrentFileValue(boolean input) { optionValues.set(0, input); }
    public void setNewValue(boolean input) { optionValues.set(2, input); }
    public boolean getSpecificValue(int index) { return optionValues.get(index); }
    public int getSpecificValueFalseVals(int index) { return falseVals.get(index); }
    public int getSpecificValueTrueVals(int index) { return trueVals.get(index); }
}
