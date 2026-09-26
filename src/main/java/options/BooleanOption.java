package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

// ShortOption is used for most of the options, which have a range of 0 to 65535. This is 2 bytes, unsigned
public class BooleanOption extends Option
{    
    // 0. current file value, 1. default value, 2. newValue;
    private ArrayList<Boolean> optionValues;
    private ArrayList<Short> trueVals;
    private ArrayList<Short> falseVals;
    
    public BooleanOption(ArrayList<Long> addresses, ArrayList<Short> falseVals, ArrayList<Short> trueVals, String optionName, String optionDesc, boolean defaultValue)
    {
        maxValue = 65535;        
        
        this.addresses = addresses;
        this.optionName = optionName;
        this.optionDesc = optionDesc;
        
        this.falseVals = falseVals;
        this.trueVals = trueVals;
        
        // 0. current file value, 1. default value, 2. newValue;
        optionValues = new ArrayList<Boolean>();
        optionValues.add(false);
        optionValues.add(defaultValue);
        optionValues.add(false);
    }

    @Override
    public void makeChanges(RandomAccessFile accessGameFile) throws IOException 
    {
        for (int i = 0; i < addresses.size(); i++)
        {
            if (optionValues.get(2))
            {
                accessGameFile.seek(addresses.get(i));
                accessGameFile.writeByte(trueVals.get(i));
            }
            else
            {
                accessGameFile.seek(addresses.get(i));
                accessGameFile.writeByte(falseVals.get(i));
            }
        }
    }
    
    public void setCurrentFileValue(boolean input)
    {
        optionValues.set(0, input);
    }
    
    public void setNewValue(boolean input)
    {
        optionValues.set(2, input);
    }
    
    public boolean getSpecificValue(int index)
    {
        return optionValues.get(index);
    }
    
    public int getSpecificValueFalseVals(int index)
    {
        return falseVals.get(index);
    }
    
    public int getSpecificValueTrueVals(int index)
    {
        return trueVals.get(index);
    }
    
}
