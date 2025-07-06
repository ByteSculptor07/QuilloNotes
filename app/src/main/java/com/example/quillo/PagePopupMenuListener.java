package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import android.widget.PopupMenu;

import com.github.bytesculptor07.quillo.Utils.GeneralUtils;
import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Page;

import java.util.List;

public class PagePopupMenuListener implements PopupMenu.OnMenuItemClickListener {
    private Page page;
    private Context context;
    private pagesortFragment parent;

    public PagePopupMenuListener(Context context, Page page, pagesortFragment parent) {
        this.page = page;
        this.context = context;
        this.parent = parent;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        NotesDatabaseHelper notesdbhelper = NotesDatabaseHelper.getInstance(context);

        if (item.getItemId() == R.id.action_delete) {
            Toast.makeText(context, "Delete Page " + GeneralUtils.getNameOfNoteId(page.getNoteId()), Toast.LENGTH_LONG).show();
            //notesdbhelper.deletePage(page);

            Page deletedPageNew = GeneralUtils.copyPage(page);
            deletedPageNew.setNumber(-page.getNumber()); //set negative number to prevent conflicts

            notesdbhelper.updatePageWithOldPage(page, deletedPageNew, SyncUtils.STATUS_SOFT_DELETION_PENDING);
            parent.refresh();

            // resort pages
            int i = 0;
            for (Page oldPage : notesdbhelper.getPagesByNoteId(page.getNoteId())) {
                Log.d("Pages", "old Number: " + oldPage.getNumber());

                Page newPage = GeneralUtils.copyPage(oldPage);
                newPage.setNumber(i);

                notesdbhelper.updatePageWithOldPage(oldPage, newPage, SyncUtils.STATUS_SYNC_PENDING);
                Log.d("Pages", "new Number: " + newPage.getNumber());
                i ++;
            }


        } else if (item.getItemId() == R.id.action_duplicate) {
            List<Page> pages = notesdbhelper.getPagesByNoteId(page.getNoteId()); //get  all pages

            //get highest number
            Page max = null;
            for (Page pg : pages) {
                if (max == null || pg.getNumber() > max.getNumber()) {
                    max = pg;
                }
            }

            Page newPage = GeneralUtils.copyPage(page);
            newPage.setUUID(null); //generate a random UUID
            newPage.setNumber(max.getNumber() + 1);
            notesdbhelper.addPage(newPage);

            parent.resortPages(max.getNumber() + 1, page.getNumber() + 1); //move page behind old page
        } else if (item.getItemId() == R.id.action_export) {
            parent.parent.exportPage(page);
        }

        parent.refresh();
        return true;
    }
}
