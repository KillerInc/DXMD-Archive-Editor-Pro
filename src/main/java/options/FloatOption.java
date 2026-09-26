package options;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class FloatOption extends Option {
    private ArrayList<Float> optionValues;
    private final float minValue, maxFloatValue;
    public FloatOption(ArrayList<Long> addresses, String optionName, String optionDesc, float defaultValue, float minValue, float maxValue) {
        this.addresses=addresses; this.optionName=optionName; this.optionDesc=optionDesc;
        this.minValue=minValue; this.maxFloatValue=maxValue;
        optionValues=new ArrayList<Float>(); optionValues.add(0f); optionValues.add(defaultValue); optionValues.add(0f);
    }
    public void setCurrentFileValue(float v){optionValues.set(0,v);} public void setNewValue(float v){optionValues.set(2,v);}
    public float getSpecificValue(int i){return optionValues.get(i);} public float getMinValue(){return minValue;} public float getMaxFloatValue(){return maxFloatValue;}
    @Override public void makeChanges(RandomAccessFile f) throws IOException {
        int bits=Float.floatToIntBits(optionValues.get(2));
        for(Long a:addresses){f.seek(a); f.writeByte(bits&255); f.writeByte((bits>>>8)&255); f.writeByte((bits>>>16)&255); f.writeByte((bits>>>24)&255);}
    }
}
