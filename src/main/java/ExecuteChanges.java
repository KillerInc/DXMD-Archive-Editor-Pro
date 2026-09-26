import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import options.Option;

public class ExecuteChanges
{
    public static void run(File gameFile, ArrayList<Option> optionData) throws IOException
    {
        try (RandomAccessFile accessGameFile = new RandomAccessFile(gameFile, "rw"))
        {
            for (Option option : optionData)
            {
                option.makeChanges(accessGameFile);
            }
        }

        Launcher.showDoneMessage();
    }
}
