package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

// InventoryXOption is used for inventory width, which is one byte, and has a max value of 16
public class InventoryXOption extends NumericOption
{        
    public InventoryXOption(ArrayList<Long> addresses, String optionName, String optionDesc, int defaultValue)
    {
        maxValue = 16;        
        
        this.addresses = addresses;
        this.optionName = optionName;
        this.optionDesc = optionDesc;
        
        // 0. current file value, 1. default value, 2. newValue;
        optionValues = new ArrayList<Integer>();
        optionValues.add(0);
        optionValues.add(defaultValue);
        optionValues.add(0);
    }
    
    @Override
    public void makeChanges(RandomAccessFile accessGameFile) throws IOException 
    {    
        for (Long address : addresses)
        {
            accessGameFile.seek(address);
            accessGameFile.writeByte(optionValues.get(2));
        }
    }
    
}
