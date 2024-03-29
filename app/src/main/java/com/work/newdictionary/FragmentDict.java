package com.work.newdictionary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.gson.Gson;

import java.util.ArrayList;

public class FragmentDict extends Fragment implements View.OnClickListener, View.OnKeyListener{
    @Nullable
    ImageView imageBtn;
    EditText txt;
    ArrayList<ListViewModel> lvModel = new ArrayList<>();
    ListView list;

    Communicate c;

    private AdView mAdView;

    public static DataSource dataSource;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dict, container, false);
        list =  view.findViewById(R.id.list_words);
        txt = view.findViewById(R.id.textView2);
        imageBtn = view.findViewById(R.id.imageView);
        imageBtn.setOnClickListener(this);
        c = (Communicate) getActivity();

        SharedPreferences sharedPref = getActivity().getSharedPreferences("my_pref", Context.MODE_PRIVATE);
        int clr = sharedPref.getInt("color", -1);
        int buttonClr = sharedPref.getInt("action_color", -16642494);
        view.setBackgroundColor(clr);
        c.background(clr);
        imageBtn.setBackgroundColor(buttonClr);

        txt.setOnKeyListener(this);

        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        dataSource = new DataSource(getContext());
        dataSource.open();

        if(dataSource.getId() < 2)
            reqId();

        Bundle bundle = getArguments();
        if(bundle != null)
            if(bundle.getBoolean("cnt", false)){
                bundle.putBoolean("cnt", false);
                itemFindFromList(bundle.getString("word", ""));
            }
        return view;
    }

    @Override
    public void onClick(View v) {
        if(v == imageBtn)
            itemFindFromText();
    }

    private void itemFindFromText() {
        c.hideKeyboard(getActivity());
        String tex = txt.getText().toString();
        req(tex);
        if(dataSource.isSet(tex) /*&& lvModel.get(0).toString() != ""*/)
            dataSource.createWord(tex);
    }

    private void itemFindFromList(String tex) {
        txt.setText(tex);
        req(tex);
    }
    private void req(final String word) {
        final ProgressDialog dialog = ProgressDialog.show(getContext(), null, "Please Wait");
        if(word.isEmpty()){
            Toast.makeText(getContext(), "Please fill it!", Toast.LENGTH_SHORT).show();
        }
        else{
            AndroidNetworking.get("http://kolayogrenci.com:1567/")
                    .addQueryParameter("user_id", Integer.toString(dataSource.getId()))
                    .addQueryParameter("word", word)
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsString(new StringRequestListener() {
                        @Override
                        public void onResponse(String  response) {
                            Log.i("FragDic",response);
                            ans(response);
                            dialog.dismiss();
                        }
                        @Override
                        public void onError(ANError error) {
                            Log.e("FragDic", error.getMessage());
                            dialog.dismiss();
                        }
                    });
        }
    }
    private void reqId() {
        AndroidNetworking.get("http://kolayogrenci.com:1567/createUser")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String  response) {
                        Log.i("FragDicID",response);
                        getAnsId(response);
                    }
                    @Override
                    public void onError(ANError error) {
                            Log.e("FragDicID", error.getMessage());
                        }
                });
    }

    private void getAnsId(String response) {
        Gson gson = new Gson();
        ResponseModelId responseModelId = gson.fromJson(response,ResponseModelId.class);
        dataSource.updateId(responseModelId.getId().getInt("id", 1));
    }

    private void ans(String response) {
        Gson gson = new Gson();
        ResponseModel responseModel = gson.fromJson(response,ResponseModel.class);
        ArrayList<String> arrayList = responseModel.getRes();
        if(arrayList != null){
            lvModel.clear();
            for(int i = 0;i < arrayList.size();i++){
                lvModel.add(new ListViewModel(arrayList.get(i)));
            }
            ContactAdapter adaptor = new ContactAdapter(getActivity(), R.layout.special_list_item, lvModel);
            list.setAdapter(adaptor);
        }
        else{
            Toast.makeText(getContext(), "\"Failed\"", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ENTER)) {
            itemFindFromText();
            return true;
        }
        return false;
    }
}
