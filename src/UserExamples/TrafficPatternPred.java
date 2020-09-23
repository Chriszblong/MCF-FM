package UserExamples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * For get the predicted speed factor
 */
public class TrafficPatternPred {
    List<Double> speed_factor_pred = new ArrayList<>();

    public TrafficPatternPred(String predFile){
        try{
            File file = new File(predFile);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tmp = null;
            while ((tmp = reader.readLine()) != null){
                speed_factor_pred.add(Double.parseDouble(tmp));
            }
            reader.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public double getSpeedFactor(int index){
        return speed_factor_pred.get(index);
    }
}
