package com.zx.album.task;

import com.zx.album.entity.AlbumImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AlbumImageManager {

    public static ArrayList<AlbumImage> getFileInfoList(String dir) {
        ArrayList<AlbumImage> fileInfoList = new ArrayList<AlbumImage>();
        File directory = new File(dir);
        if (!directory.exists())
            return null;

        File[] files = directory.listFiles();
        // Consider the situation that has none initial data
        if (files == null)
            return null;

        for (File file : files) {
            AlbumImage fi = new AlbumImage();
            if (file.isDirectory()) {
//                fi.setParentFolderPath(file.getPath());
//                fi.setContinuePics(getFileInfoList(file.getPath()));
//                if (fi.getContinuePics().size() == 0)
//                    continue;
//                FileInfo newestFile = fi.getContinuePics().get(0);
//                if (newestFile != null) {
//                    fi.setIsContinuePics(true);
//                    fi.setName(newestFile.getName());
//                    fi.setPath(newestFile.getPath());
//                    fi.setLastModified(newestFile.getLastModified());
//                    fi.setMineType(newestFile.getMineType());
//                    fileInfoList.add(fi);
//                }
//                continue;
            }

            fi.setName(file.getName());
            fi.setPath(file.getPath());
            fi.setLastModified(file.lastModified());
            if (fi.getName().endsWith("jpg")) {
                fi.setMineType("image/*");
            } else if (fi.getName().endsWith("png")) {
                fi.setMineType("image/*");
            } else if (fi.getName().endsWith("mp4")) {
                fi.setMineType("video/*");
            } else {
                fi.setMineType("unknown");
            }
            fileInfoList.add(fi);
        }
        Collections.sort(fileInfoList, new ComparatorByLastModifiedTime());

        return fileInfoList;
    }

    /**
     * rank as time ,the newest rank first
     */
    private static class ComparatorByLastModifiedTime implements
            Comparator<AlbumImage> {

        @Override
        public int compare(AlbumImage fileInfo1, AlbumImage fileInfo2) {
            long diff = fileInfo1.getLastModified() - fileInfo2.getLastModified();

            return diff < 0 ? 1 : diff > 0 ? -1 : 0;
        }
    }

}


