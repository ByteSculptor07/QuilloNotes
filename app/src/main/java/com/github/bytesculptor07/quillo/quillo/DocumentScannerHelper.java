package com.github.bytesculptor07.quillo;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.websitebeaver.documentscanner.DocumentScanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;


public class DocumentScannerHelper {
    private static DocumentScannerHelper instance;
    private ActivityResultLauncher<Intent> launcher;
    private DocumentScanner documentScanner;
    private ComponentActivity activity;

    // Store registered fragments and their callbacks
    private final Map<String, SuccessCallback> fragmentCallbacks = new HashMap<>();

    public interface SuccessCallback {
        void onScanSuccess(ArrayList<String> scannedImages);
    }

    private DocumentScannerHelper(ComponentActivity activity) {
        this.activity = activity;

        documentScanner = new DocumentScanner(
                activity,
                croppedImageResults -> {
                    if (!croppedImageResults.isEmpty()) {
                        String imagePath = croppedImageResults.get(0);

                        // Call the callback of the currently visible fragment
                        /*
                        String visibleFragmentTag = getVisibleFragmentTag();
                        if (visibleFragmentTag != null && fragmentCallbacks.containsKey(visibleFragmentTag)) {
                            fragmentCallbacks.get(visibleFragmentTag).onScanSuccess(croppedImageResults);
                        } else if (visibleFragmentTag == null) {
                            Toast.makeText(activity, "Scanned: " + imagePath, Toast.LENGTH_SHORT).show();
                        }
                        */
                        Fragment fragment = getVisibleFragmentTag();
                        if (fragment != null) {
                            fragmentCallbacks.get(fragment.getTag()).onScanSuccess(croppedImageResults);
                            //((drawFragment) fragment).scanSuccess(croppedImageResults);
                        }
                    }
                    return Unit.INSTANCE;
                },
                errorMessage -> {
                    Log.e("DocumentScannerHelper", "Error: " + errorMessage);
                    return Unit.INSTANCE;
                },
                () -> {
                    Log.i("DocumentScannerHelper", "User canceled the scan");
                    return Unit.INSTANCE;
                },
                "imageFilePath",
                true,
                24,
                100
        );

        initialize();
    }

    public void setAct(FragmentActivity activity) {
        this.activity = (ComponentActivity) activity;
    }

    public static synchronized DocumentScannerHelper getInstance(ComponentActivity activity) {
        if (instance == null) {
            instance = new DocumentScannerHelper(activity);
        }
        return instance;
    }

    public void registerFragment(String tag, SuccessCallback callback) {
        fragmentCallbacks.put(tag, callback);
    }

    public void unregisterFragment(String tag) {
        fragmentCallbacks.remove(tag);
    }

    public void start() {
        if (launcher != null) {
            launcher.launch(documentScanner.createDocumentScanIntent());
        }
    }

    private void initialize() {
        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (documentScanner != null) {
                        documentScanner.handleDocumentScanIntentResult(result);
                    } else {
                        Log.e("DocumentScannerHelper", "DocumentScanner is not initialized.");
                    }
                }
        );
    }

    private Fragment getVisibleFragmentTag() {
        /*
        FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();

        for (Fragment fragment : fragments) {
            if (((drawFragment) fragment).getVisibility()) {
                return fragment.getTag(); // Get the tag of the visible fragment
            }
        }
        return null;
        */
        return ((drawActivity) activity).getVisibleFragment();
    }
}
