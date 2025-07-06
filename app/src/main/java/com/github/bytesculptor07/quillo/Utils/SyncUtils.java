package com.github.bytesculptor07.quillo.Utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.github.bytesculptor07.quillo.NoteParser;
import com.github.bytesculptor07.quillo.database.FoldersDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Folder;
import com.github.bytesculptor07.quillo.models.Page;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncUtils {
    public static final int STATUS_SYNCED = 0;
    public static final int STATUS_SYNC_PENDING = 1;
    public static final int STATUS_ATTACHMENT_SYNC_PENDING = 2;
    public static final int STATUS_SOFT_DELETED = 3;
    public static final int STATUS_SOFT_DELETION_PENDING = 4;
    public static final int STATUS_HARD_DELETED = 5;
    public static final int STATUS_HARD_DELETION_PENDING = 6;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface SyncCallback {
        void onSuccess();
        void onError(Exception e);
    }


    // === General ===

    public static void syncAllAsynchronous(Context context, DbxClientV2 client, SyncCallback callback) {
        executor.execute(() -> {
            try {
                syncAll(context, client);
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void syncAll(Context context, DbxClientV2 client) {
        try {
            syncNotes(context, client);
            syncFolder(context, client);
        } catch (DbxException e) {
            //Toast.makeText(context, "error with Dropbox during syncing: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("Sync", "error with Dropbox during syncing: " + e.getMessage());
        } catch (IOException e) {
            //Toast.makeText(context, "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("Sync", "IOException: " + e.getMessage());
        }
    }

    public static void syncNotes(Context context, DbxClientV2 client) throws DbxException, IOException {
        uploadNoteChanges(context, client);
        downloadNoteChanges(context, client);
        downloadAttachments(context, client);
    }

    public static void syncFolder(Context context, DbxClientV2 client) throws DbxException, IOException {
        uploadFolderChanges(context, client);
        downloadFolderChanges(context, client);
    }


    // === Folder ===

    public static void uploadFolderChanges (Context context, DbxClientV2 client) throws DbxException, IOException {
        FoldersDatabaseHelper folderdbhelper = FoldersDatabaseHelper.getInstance(context);
        List<Folder> folders = folderdbhelper.getFolderWithChanges();
        for (Folder folder : folders) {
            String contentHash = uploadFolder(folder, client);
            folderdbhelper.removeFolderChanges(folder);
            folderdbhelper.updateHash(folder, contentHash);
        }
    }

    public static void downloadFolderChanges (Context context, DbxClientV2 client) throws DbxException, IOException {
        FoldersDatabaseHelper folderdbhelper = FoldersDatabaseHelper.getInstance(context);

        for (Metadata md : listFolder(client, "/folder")) {
            String path = ((FileMetadata) md).getPathDisplay();
            String cloudHash = ((FileMetadata) md).getContentHash();
            //Log.d("Database", "Querying hash with noteId=" + NoteParser.extractNoteId(path) + " and number=" + NoteParser.extractNumber(path));
            //Log.d("Sync1", GeneralUtils.getFolderIdOfPath(path));
            String localHash = folderdbhelper.getHashByFolderId(GeneralUtils.getFolderIdOfPath(path));

            if (cloudHash != null && !cloudHash.equals(localHash)) {
                try {
                    Log.d("Sync", "downloading Folder");
                    downloadFolder(path, client, folderdbhelper);
                } catch (IOException | DbxException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    private static String uploadFolder(Folder folder, DbxClientV2 client) throws IOException, DbxException {
        String content = folder.exportToJson();
        InputStream in = new ByteArrayInputStream(content.getBytes());

        String filepath = "/folder/" + String.valueOf(folder.getId()) + ".qdata";
        FileMetadata metadata = client.files().uploadBuilder(filepath)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(in);

        return metadata.getContentHash();
    }

    public static void downloadFolder(String path, DbxClientV2 client, FoldersDatabaseHelper folderdbhelper) throws IOException, DbxException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            FileMetadata metadata = client.files()
                    .downloadBuilder(path)
                    .download(outputStream);

            String fileContentAsString = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fileContentAsString = outputStream.toString(StandardCharsets.UTF_8);
            }

            String hash = metadata.getContentHash();

            Folder folder = Folder.importFromJson(fileContentAsString);
            if (!folderdbhelper.folderExists(folder)) {
                folderdbhelper.addFolder(folder, SyncUtils.STATUS_SYNCED);
            } else {
                folderdbhelper.updateFolder(folder, SyncUtils.STATUS_SYNCED);
            }

            folderdbhelper.updateHash(folder, hash);

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    // === Attachments ===

    public static void downloadAttachments(Context context, DbxClientV2 client) throws IOException, DbxException {
        for (Metadata md : listFolder(client, "/attachments")) {
            String name = ((FileMetadata) md).getName();
            String path = ((FileMetadata) md).getPathDisplay();
            //Log.d("Sync", path);

            File file = null;
            if (name.endsWith("pdf")) {
                file = new File(
                        context.getExternalFilesDir(
                                Environment.DIRECTORY_DOCUMENTS),
                        name);
            } else {
                file = new File(
                        context.getExternalFilesDir(
                                Environment.DIRECTORY_PICTURES),
                        name);
            }

            if (!file.exists()) {
                downloadAttachment(path, file, client);
            }
        }
    }

    private static void downloadAttachment(String path, File file, DbxClientV2 client) throws IOException, DbxException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            FileMetadata metadata = client.files()
                    .downloadBuilder(path)
                    .download(outputStream);
            //Log.d("Sync", "Downloading attachment to path: " + file.getAbsolutePath());
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    // === Notes ===

    public static void downloadNoteChanges(Context context, DbxClientV2 client) {
        NotesDatabaseHelper notesdbhelper = NotesDatabaseHelper.getInstance(context);

        for (Metadata md : listFolder(client, "/notes")) {
            String path = ((FileMetadata) md).getPathDisplay();
            //Log.d("noteSync", "2: " + path);
            String cloudHash = ((FileMetadata) md).getContentHash();
            //Log.d("Database", "Querying hash with noteId=" + NoteParser.extractNoteId(path) + " and number=" + NoteParser.extractNumber(path));
            Log.d("Sync", "Querying with UUID: " + NoteParser.extractUUID(path));
            //Log.d("Database", "Status: " + NoteParser.extractStatus(path));
            String localHash = notesdbhelper.getHashByUUID(NoteParser.extractUUID(path));
            /*
            String localHash = notesdbhelper.getHashByNoteIDAndNumber(
                    NoteParser.extractNoteId(path),
                    NoteParser.extractNumber(path)
            );

             */

            if (cloudHash != null && !cloudHash.equals(localHash)) {
                try {
                    Log.d("Sync", "downloading Note");
                    //Log.d("Sync", "localHash: " + localHash);
                    downloadPage(path, client, notesdbhelper);
                } catch (IOException | DbxException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void uploadNoteChanges(Context context, DbxClientV2 client) throws IOException, DbxException {
        NotesDatabaseHelper notesdbhelper = NotesDatabaseHelper.getInstance(context);
        List<Page> pages = notesdbhelper.getPagesWithChanges();
        for (Page page : pages) {
            if (page.getStatus() == STATUS_ATTACHMENT_SYNC_PENDING) {
                uploadAttachment(context, page, client);
            }

            //notesdbhelper.removePageChanges(page);
            //Log.d("Sync", "status: " + page.getStatus());
            /*
            if (page.getStatus() == STATUS_SOFT_DELETION_PENDING) {
                notesdbhelper.updatePageWithOldPage(page, page, SyncUtils.STATUS_SOFT_DELETED); //use unique id
                deletePageFromDropbox(page, client); // delete old page before creating a new one with a different negative number
                //Log.d("Sync", "setting status: " + STATUS_SOFT_DELETED);
            } else if (page.getStatus() == STATUS_HARD_DELETION_PENDING) {
                notesdbhelper.updatePageWithOldPage(page, page, SyncUtils.STATUS_HARD_DELETED); //use unique id
                //Log.d("Sync", "setting status: " + STATUS_HARD_DELETED);
            } else {

             */
                notesdbhelper.updatePageWithOldPage(page, page, invertStatus(page.getStatus())); //use unique id
            //}

            String contentHash = uploadPage(page, client); //maybe before if statements, prevent app from downloading again after status set
            notesdbhelper.updateHash(page, contentHash);
        }

    }

    private static void uploadAttachment(Context context, Page page, DbxClientV2 client) throws IOException, DbxException {
        if (page.getPdf() != null) {
            File pdf = new File(
                    context.getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS),
                    page.getPdf().replace("file:", ""));
            uploadFile(pdf, client);
        }
        if (page.getBackground() != null) {
            File image = new File(page.getBackground().replace("file:", ""));
            uploadFile(image, client);
        }
    }

    private static void uploadFile(File file, DbxClientV2 client) throws IOException, DbxException {
        FileInputStream in = new FileInputStream(file);
        String filepath = "/attachments/" + file.getName();

        FileMetadata metadata = client.files().uploadBuilder(filepath)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(in);

        in.close();
    }

    private static void deletePageFromDropbox(Page page, DbxClientV2 client) {
        String path = "/notes" + page.getNoteId() + "_" + -(page.getNumber()) + ".qdoc." + STATUS_SYNCED;
        try {
            Metadata metadata = client.files().delete(path);
        } catch (DbxException e) {
            Log.d("Sync", "file not found: " + path);
        }
    }

    private static String uploadPage(Page page, DbxClientV2 client) throws IOException, DbxException {
        String content = page.exportToJson();
        InputStream in = new ByteArrayInputStream(content.getBytes());

        int newStatus = STATUS_SYNCED;
        if (page.getStatus() == STATUS_SOFT_DELETION_PENDING) {
            newStatus = STATUS_SOFT_DELETED;
        } else if (page.getStatus() == STATUS_HARD_DELETION_PENDING) {
            newStatus = STATUS_HARD_DELETED;
        }

        //String filepath = "/notes" + page.getNoteId() + "_" + page.getNumber() + ".qdoc." + newStatus;
        String filepath = "/notes/" + page.getUUID() + ".qpage";
        FileMetadata metadata = client.files().uploadBuilder(filepath)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(in);

        return metadata.getContentHash();
    }

    public static void downloadPage(String path, DbxClientV2 client, NotesDatabaseHelper notesdbhelper) throws IOException, DbxException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            FileMetadata metadata = client.files()
                    .downloadBuilder(path)
                    .download(outputStream);

            String fileContentAsString = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fileContentAsString = outputStream.toString(StandardCharsets.UTF_8);
            }

            String hash = metadata.getContentHash();

            Page page = Page.importFromJson(fileContentAsString);
            if (!notesdbhelper.pageExist(page, true)) {
                notesdbhelper.addPage(page, invertStatus(page.getStatus()));
            } else {
                notesdbhelper.updatePage(page, invertStatus(page.getStatus()), true);
                //notesdbhelper.updatePage(page, SyncUtils.STATUS_SYNCED);
            }

            page.setStatus(invertStatus(page.getStatus()));
            notesdbhelper.updateHash(page, hash);
            //Log.d("Sync", "setting hash: " + hash);

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    // === Helper ===


    private static int invertStatus(int status) {
        int result = STATUS_SYNCED;

        if (status == STATUS_SOFT_DELETION_PENDING) {
            result = STATUS_SOFT_DELETED;
        } else if (status == STATUS_HARD_DELETION_PENDING) {
            result = SyncUtils.STATUS_HARD_DELETED;
        }

        return result;
    }


    private static List<Metadata> listFolder(DbxClientV2 client, String path) {
        List<Metadata> funcresult = new ArrayList<>();
        try {
            ListFolderResult result = client.files().listFolder(path);

            for (Metadata metadata : result.getEntries()) {
                if (metadata instanceof FolderMetadata) {
                    List<Metadata> subResult = new ArrayList<>();
                    subResult = listFolder(client, metadata.getPathLower());
                    funcresult.addAll(subResult);
                } else if (metadata instanceof FileMetadata) {
                    //String filePath = metadata.getPathLower();
                    funcresult.add(metadata);
                    //Log.d("listFolderRecursive", "File added: " + filePath);
                }
            }

            while (result.getHasMore()) {
                result = client.files().listFolderContinue(result.getCursor());
                for (Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FolderMetadata) {
                        List<Metadata> subResult = new ArrayList<>();
                        subResult = listFolder(client, metadata.getPathLower());
                        funcresult.addAll(subResult);
                    } else if (metadata instanceof FileMetadata) {
                        //String filePath = metadata.getPathLower();
                        funcresult.add(metadata);
                        //Log.d("listFolderRecursive", "File added: " + filePath);
                    }
                }
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }

        return funcresult;
    }

}
