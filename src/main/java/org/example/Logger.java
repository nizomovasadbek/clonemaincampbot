package org.example;

import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiersOrPrimitiveType;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class Logger {

    /**
     * error log
     */

    private final String E_LOG = "[\tERROR\t]\t";
    private final String I_LOG = "[\tINFO\t]\t";

    private void writeFile(String text){
        try {
            File f = new File("src/main/resources/application.log");
            PrintWriter pw = new PrintWriter(f);
            pw.println(text);
            pw.close();
            pw.flush();
            if (!f.exists()) {
                f.createNewFile();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String getCurrentTime(){
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = date.format(dateFormatter);
        return formattedDate + "\t";
    }



    public void loge(String errMessage){
        String error_message = E_LOG + getCurrentTime() + errMessage;
        writeFile(error_message);
    }

    public void logi(String infoMessage){
        String info_message = I_LOG + getCurrentTime() + infoMessage;
        writeFile(info_message);
    }

}
