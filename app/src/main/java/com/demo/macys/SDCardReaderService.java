package com.demo.macys;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SDCardReaderService extends Service {

    private NotificationCompat.Builder builder;
    private NotificationManager nm;
    private int count;
    private HashMap<String, Integer> extensionOccurances;

    public SDCardReaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initStatusBarNotification();
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initStatusBarNotification(){
        nm = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("SD Card Scan")
                .setContentText("In Progress")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent);
    }

    Thread thread = new Thread() {
        @Override
        public void run() {
            super.run();
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                count = 0;
                calculateNumberOfFiles(Environment.getExternalStorageDirectory().getAbsolutePath());
                Intent scanStartedIntent = new Intent();
                scanStartedIntent.setAction(MainActivity.SCAN_STARTED_ACTION);
                scanStartedIntent.putExtra(MainActivity.MAX, count);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(scanStartedIntent);
                files = new ArrayList<>();
                currentFileNumber = 0;
                extensionOccurances = new HashMap<>();
                builder.setProgress(count, 0, false);
                nm.notify(1, builder.build());
                recursivelyReadSDCard(Environment.getExternalStorageDirectory().getAbsolutePath());
                builder.setProgress(0, 0, false);
                builder.setContentText("Scanning Completed");
                Collections.sort(files, fileSizeComparator);
                Intent intent = new Intent(MainActivity.SCAN_FINISHED_ACTION);
                int size = 0;
                for(int i = 0 ; i < files.size(); i++){
                    size+=files.get(i).getSize();
                }
                double avgFileSize = 0;
                if(files.size()>0)
                    avgFileSize = size/files.size();
                ArrayList<FileInfo> tempList = new ArrayList<>();
                if(files.size()>10){
                    tempList.addAll(files.subList(0, 10));
                } else {
                    tempList.addAll(files);
                }
                intent.putParcelableArrayListExtra(MainActivity.FILES_INFO, tempList);
                intent.putExtra(MainActivity.AVG_FILE_SIZE, avgFileSize);
                intent.putExtra(MainActivity.EXTN_INFO, (LinkedHashMap<String, Integer>)entriesSortedByValues(extensionOccurances));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                stopSelf();
            } else {
                Toast.makeText(getApplicationContext(), "Media is not available", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private Map<String, Integer> entriesSortedByValues(Map<String,Integer> map) {
        List<Map.Entry<String,Integer>> sortedEntries = new ArrayList<Map.Entry<String,Integer>>(map.entrySet());
        Collections.sort(sortedEntries,
                new Comparator<Map.Entry<String,Integer>>() {
                    @Override
                    public int compare(Map.Entry<String,Integer> e1, Map.Entry<String,Integer> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );
        Map<String, Integer> tempMap = new LinkedHashMap<>();
        for(int i = 0; i <  sortedEntries.size(); i++){
            if(i>=5) break;
            tempMap.put(sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue());
        }
        return tempMap;
    }


    private Comparator<FileInfo> fileSizeComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            return lhs.getSize() > rhs.getSize() ? -1: lhs.getSize() < rhs.getSize() ? 1 : 0;
        }
    };

    private void calculateNumberOfFiles(String path){
        File file = new File(path);
        if (file.exists())
            if (file.isDirectory()) {
                File[] innerFiles = file.listFiles();
                if (innerFiles != null)
                    for (int i = 0; i < innerFiles.length; i++) {
                        File innerFile = innerFiles[i];
                        if(innerFile.isDirectory()){
                            calculateNumberOfFiles(innerFile.getAbsolutePath());
                        } else if(innerFile.isFile()){
                            if (!innerFile.getName().startsWith(".")) {
                                if(innerFile.getName().contains(".")) {
                                    count++;
                                }
                            }
                        }
                    }
            }
    }

    private ArrayList<FileInfo> files;
    private int currentFileNumber;
    private void recursivelyReadSDCard(String path) {
        File file = new File(path);
        if (file.exists())
            if (file.isDirectory()) {
                File[] innerFiles = file.listFiles();
                if (innerFiles != null && innerFiles.length>0)
                    for (int i = 0; i < innerFiles.length; i++) {
                        if(innerFiles[i].isDirectory()) {
                            recursivelyReadSDCard(innerFiles[i].getAbsolutePath());
                        } else if(innerFiles[i].isFile()){
                            FileInfo stat = new FileInfo();
                            stat.setName(innerFiles[i].getName());
                            if (!innerFiles[i].getName().startsWith(".")) {
                                if(innerFiles[i].getName().contains(".")) {
                                    currentFileNumber++;
                                    builder.setProgress(count, currentFileNumber, false);
                                    builder.setContentText("Completed "+currentFileNumber+" of "+count);
                                    nm.notify(1, builder.build());
                                    Intent scanProgressIntent = new Intent();
                                    scanProgressIntent.setAction(MainActivity.SCAN_PROGRESS_ON_ACTION);
                                    scanProgressIntent.putExtra(MainActivity.PROGRESS, currentFileNumber);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(scanProgressIntent);
                                    String extension = innerFiles[i].getName().substring(innerFiles[i].getName().lastIndexOf(".") + 1);
                                    if (!extensionOccurances.containsKey(extension)) {
                                        extensionOccurances.put(extension, 1);
                                    } else {
                                        extensionOccurances.put(extension, extensionOccurances.get(extension) + 1);
                                    }
                                    stat.setSize(innerFiles[i].length());
                                    files.add(stat);
                                }
                            }
                        }
                    }
            }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(thread != null && thread.isAlive()) try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            nm.cancel(1);
        }catch (Exception e){

        }
    }
}