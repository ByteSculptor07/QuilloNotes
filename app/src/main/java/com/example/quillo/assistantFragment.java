package com.github.bytesculptor07.quillo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class assistantFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }
}
/*
package com.github.bytesculptor07.quillo;

import android.os.Bundle;
import android.widget.ProgressBar;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.opengl.program.GlProgram;
import java.util.Arrays;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class assistantFragment extends Fragment {

    //static {
    //    System.loadLibrary("faster_rwkv_jni");
    //}
    
    //String MODEL_HASH = "3c5a9e92e0c100128b538bae7700ebf1070b66cb8ab785b92cf7e015b86ad646";
    //String DOWNLOAD_URL = "https://huggingface.co/Bytesculptor07/RWKV/resolve/main/RWKV-4-World-0.1B-v1-20230520-ctx4096-ncnn.bin";
    String DOWNLOAD_URL = "https://huggingface.co/KaleiNeely/fr-models/resolve/main/RWKV-5-World-1B5-v2-20231025-ctx4096/ncnn/fp16/RWKV-5-World-1B5-v2-20231025-ctx4096-fp16.bin";
    String INFO_URL = "https://huggingface.co/KaleiNeely/fr-models/raw/main/RWKV-5-World-1B5-v2-20231025-ctx4096/ncnn/fp16/RWKV-5-World-1B5-v2-20231025-ctx4096-fp16.bin";
    //String MODEL_NAME = "RWKV-4-World-0.1B-v1-20230520-ctx4096-ncnn.bin";
    String MODEL_NAME ="RWKV-5-World-1B5-v2-20231025-ctx4096-fp16.bin";
    View fragmentView;
    EditText promptInput;
    private Model model;
    private Tokenizer tokenizer;
    private Sampler sampler;
    
    int BOS_ID = 2;
    int EOS_ID = 3;
    
    float temperature = 0.7f;
    int top_k = 10;
    float top_p = 0.9f;
    
    String history = "";
    long time1, time2, tokenizerTime, modelTime;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }
    
    private static int[] add2BeginningOfArray(int[] elements, int element) {
        int[] newArray = Arrays.copyOf(elements, elements.length + 1);
        newArray[0] = element;
        System.arraycopy(elements, 0, newArray, 1, elements.length);

        return newArray;
    }
    
    public static File copyAssetToFile(Context context, String assetName) {
        File outFile = new File(context.getFilesDir(), assetName);
        if (outFile.exists()) return outFile;

        try (InputStream in = context.getAssets().open(assetName);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outFile;
    }
    
    public boolean downloadModel(ProgressBar pg, Button sendButton, TextView resultText) {
        //File outFile = new File(getContext().getFilesDir(), MODEL_NAME);
        //if (outFile.exists()) initModel(resultText, sendButton);
        
        File tempFile = new File(getContext().getFilesDir(), "model.tmp");
        File finalFile = new File(getContext().getFilesDir(), MODEL_NAME);
        
        if (finalFile.exists()) {
            initModel(resultText, sendButton);
            return false;
        }
        
        
        InternalFileDownloader.downloadFileToInternal(getContext(), DOWNLOAD_URL, INFO_URL, "model.tmp", pg, resultText, new InternalFileDownloader.DownloadCallback() {
            @Override
            public void onDownloadComplete(File downloadedFile) {
                // Handle the downloaded file
                //System.out.println("Download complete: " + downloadedFile.getAbsolutePath());
                Toast.makeText(getContext(), "download complete", Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
                    
                tempFile.renameTo(finalFile);
                initModel(resultText, sendButton);
            }
        
            @Override
            public void onDownloadFailed(String error) {
                // Handle the error
                //System.err.println("Download failed: " + error);
                    Toast.makeText(getContext(), "download failed", Toast.LENGTH_SHORT).show();
            }
        });
        
        return true;
    }
    
    public void initModel(TextView resultText, Button sendButton) {
        try{
        Toast.makeText(getContext(), "init model", Toast.LENGTH_SHORT).show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                //String input = "User: Do you like cats?\nAssistant:";
                //copyAssetToFile(getContext(), "RWKV-4-World-0.1B-v1-20230520-ctx4096-ncnn.param");
                copyAssetToFile(getContext(), "RWKV-5-World-1B5-v2-20231025-ctx4096-fp16.param");
                File modelFile = copyAssetToFile(getContext(), MODEL_NAME);
                //File tokenizerFile = copyAssetToFile(getContext(), "RWKV-4-World-0.1B-v1-20230520-ctx4096_ncnn_fp16_tokenizer");
                File tokenizerFile = copyAssetToFile(getContext(), "tokenizers_world_toknizer.bin");
                String path = modelFile.getAbsolutePath();
        
                model = new Model(path.substring(0, path.length() - 4), "ncnn fp16");
                tokenizer = new Tokenizer(tokenizerFile.getAbsolutePath());
                sampler = new Sampler();
                    
                mainHandler.post(() -> sendButton.setEnabled(true));
            } catch (Exception e) {
                resultText.setText("Error: " + e.getMessage());
            }
        });
            } catch (Exception e) {
                Toast.makeText(getContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
    }
    
    public void generateResponse(TextView tv, Button sendButton) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                mainHandler.post(() -> sendButton.setEnabled(false));
                //String input = "User: Do you like cats?\nAssistant:";
                String input = history + "Bob: " + promptInput.getText() + "\n\nAlice:";
                String result = input;
                
                    // Start time before tokenization
                time1 = System.currentTimeMillis();
                
                    
                    
                // Tokenizer operation
                int[] input_ids = tokenizer.encode(input);
                input_ids = add2BeginningOfArray(input_ids, BOS_ID);
                
                // Time after tokenization and before model run
                time2 = System.currentTimeMillis();
                
                // Model operation
                float[] logits = model.run(input_ids);
                
                // Time after model run
                long time3 = System.currentTimeMillis();
                
                // Calculate times
                tokenizerTime = time2 - time1;
                modelTime = time3 - time2;
                
                // Display results using Toast
                mainHandler.post(() -> 
                    Toast.makeText(
                        getContext(),
                        "Tokenizer Time: " + tokenizerTime + "ms\nModel Time: " + modelTime + "ms",
                        Toast.LENGTH_LONG
                    ).show()
                );
                    
                    

                //int[] input_ids = tokenizer.encode(input);
                //input_ids = add2BeginningOfArray(input_ids, BOS_ID);
                //float[] logits = model.run(input_ids);

                for (int i = 0; i < 256; i++) {
                    int output_id = sampler.sample(logits, temperature, top_k, top_p);
                    if (output_id == EOS_ID) {
                        break;
                    }
                    String output = tokenizer.decode(output_id);
                    result += output;
                        
                    if (result.endsWith("\n\n")) {
                        break;
                    }
                    
                    final String part_res = result;
                    mainHandler.post(() -> tv.setText(part_res));
                    logits = model.run(output_id);
                }
                history += result;
                mainHandler.post(() -> sendButton.setEnabled(true));
            } catch (Exception e) {
                mainHandler.post(() -> {
                tv.setText("Error: " + e.getMessage());
                sendButton.setEnabled(true);
                });
            }
        });
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        fragmentView = view;
        
        TextView resultText = view.findViewById(R.id.textView);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        Button sendButton = view.findViewById(R.id.sendButton);
        promptInput = view.findViewById(R.id.prompt);
        
        //downloadModel(progressBar, sendButton, resultText);
        
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateResponse(resultText, sendButton);
            }
        });
    }
}
*/