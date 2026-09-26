package options;

import java.util.ArrayList;

// This abstract class is numeric option, as opposed to the boolean option
public abstract class NumericOption extends Option
{
    // 0. current file value, 1. default value, 2. newValue;
    protected ArrayList<Integer> optionValues;
    
    public void setCurrentFileValue(int input)
    {
        optionValues.set(0, input);
    }
    
    public void setNewValue(int input)
    {
        optionValues.set(2, input);
    }
    
    public int getSpecificValue(int index)
    {
        return optionValues.get(index);
    }
    
    public int getMaxValue() {return maxValue;}
}
