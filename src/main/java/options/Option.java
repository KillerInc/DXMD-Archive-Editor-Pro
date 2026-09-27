package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public abstract class Option {
    protected ArrayList<Long> addresses;
    protected String optionName, optionDesc;
    protected int maxValue;
    private boolean mixedCurrentValues;

    public ArrayList<Long> getAddresses() { return addresses; }
    public String getOptionName() { return optionName; }
    public String getOptionDesc() { return optionDesc; }
    public boolean hasMixedCurrentValues() { return mixedCurrentValues; }
    public void setMixedCurrentValues(boolean mixed) { mixedCurrentValues = mixed; }

    public abstract void makeChanges(RandomAccessFile accessGameFile) throws IOException;
}
