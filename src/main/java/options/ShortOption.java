package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

// ShortOption is used for most of the options, which have a range of 0 to 65535. This is 2 bytes, unsigned
public class ShortOption extends NumericOption
{        
    public ShortOption(ArrayList<Long> addresses, String optionName, String optionDesc, int defaultValue)
    {
        maxValue = 65535;        
        
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
            short secondByte = (short) (optionValues.get(2) / 256);
            short firstByte = (short) (optionValues.get(2) % 256);
            accessGameFile.seek(address);
            accessGameFile.writeByte(firstByte);
            accessGameFile.seek(address + 1);
            accessGameFile.writeByte(secondByte);
        }
    }    
    
}
