package com.example.minim.cit_prototype.Module.Fragment.Main;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.minim.cit_prototype.ChartDrawer;
import com.example.minim.cit_prototype.Custom.CustomViewPager;
import com.example.minim.cit_prototype.R;
import com.example.minim.cit_prototype.User;
import com.example.minim.cit_prototype.VoiceListener;
import com.github.bassaer.chatmessageview.model.Message;
import com.github.bassaer.chatmessageview.view.ChatView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Common.CommonEventBusObject;
import Common.ConstVariables;
import Common.Utils.CommonUtils;
import Dialog.CommonDialog;
import Dialog.TrainingDialog;
import Utils.PreferencesManager;
import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.GsonFactory;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

public class MainFragment extends Fragment implements View.OnClickListener {
    private final String TAG = MainFragment.class.getSimpleName();
    final String FRIEND_TOKEN = "d26cfd6907fa411b9c72aea1159e8d07";
    final String CHILD_TOKEN = "fdf9f71121544dbf8693b645623f2aff";

    private final int IS_CLICKABLE_MSG = 300;
    private final int IS_GRAPH_VIEW = 301;

    //For Dialogflow
    private Gson gson = GsonFactory.getGson();
    private AIDataService aiDataService;

    //for view
    private ChatView chatView;
    //for chat
    private User myAccount;
    private User citBot;
    private int mCurrentAgentType;

    /* Test Page*/
    private RelativeLayout mTestLayout;
    private CustomViewPager mViewPager;
    private ImageView[] mProgress;

    private VoiceListener voiceListener;

    /* Test, Training chat image */
    private View mTestStartView;
    private Bitmap mTestStartImage;
    private View mTrainingStartView;
    private Bitmap mTrainingStartImage;
    private View mGraphView;
    private Bitmap mGraphStartImage;
    private View mStampView;
    private Bitmap mStampViewImage;

    /* Selected Training Mode*/
    private int mCurrentTrainingMode;

    /* Voice */
    private PopupWindow mPopupWindow;
    private ImageView mVoiceButton;
    private LinearLayout mVoiceInputLayout;
    private ImageView mVoiceAnimation;
    private AnimationDrawable mAnimationDrawable;
    private TextView mVoiceResultText;
    private ImageView mConfirmButton;


    //User Mode
    private int mCurrentSelectedMode = ConstVariables.Companion.getUSER_SELECT_TRAINING();

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        voiceListener = new VoiceListener(getActivity());
        mCurrentAgentType = PreferencesManager.INSTANCE.loadIntegerSharedPreferences(getActivity(), ConstVariables.Companion.getPREF_KEY_AGENT_TYPE());
        initService(mCurrentAgentType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);
        mTestStartView = inflater.inflate(R.layout.layout_test_start, container, false);
        mTrainingStartView = inflater.inflate(R.layout.layout_training_start, container, false);
        mGraphView = inflater.inflate(R.layout.layout_graph_start, container, false);
        mStampView = inflater.inflate(R.layout.layout_stamp_start, container, false);
        initializeUI(view, container);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTestStartImage = CommonUtils.Companion.viewToBitmap(getActivity(), mTestStartView
                .findViewById(R.id.layout_test_start));
        mTrainingStartImage = CommonUtils.Companion.viewToBitmap(getActivity(), mTrainingStartView.findViewById(R.id.layout_traiining_start));
        mGraphStartImage = CommonUtils.Companion.viewToBitmap(getActivity(), mGraphView.findViewById(R.id.layout_graph_starts));
        mStampViewImage = CommonUtils.Companion.viewToBitmap(getActivity(), mStampView.findViewById(R.id.layout_test_start));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    private void initializeUI(final View v, final ViewGroup container) {
        Log.d(TAG, "##### initializeUI #####");

        mTestLayout = v.findViewById(R.id.layout_test);
        mVoiceInputLayout = v.findViewById(R.id.layout_voice_input);
        mViewPager = v.findViewById(R.id.viewpager_traning);
        mViewPager.setPagingEnabled(false);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(mPagerChangeListener);
        mVoiceButton = v.findViewById(R.id.btn_test_voice);
        mVoiceButton.setOnClickListener(this);
        mVoiceAnimation = v.findViewById(R.id.imageview_voice_anim);
        mAnimationDrawable = (AnimationDrawable) mVoiceAnimation.getBackground();


        mProgress = new ImageView[]{
                v.findViewById(R.id.progress_1)
                , v.findViewById(R.id.progress_2)
                , v.findViewById(R.id.progress_3)
                , v.findViewById(R.id.progress_4)
                , v.findViewById(R.id.progress_5)
                , v.findViewById(R.id.progress_6)
        };
        mProgress[0].setSelected(true);

        int myId = 0;
        Bitmap usrIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_user);
        String myName = "Fish";
        myAccount = new User(myId, myName, usrIcon);

        int botId = 1;
        Bitmap agentIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_user);
        if (mCurrentAgentType == ConstVariables.Companion.getPREF_AGENT_TYPE_FRIEND()) {
            agentIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_bot_1_n);
        } else if (mCurrentAgentType == ConstVariables.Companion.getPREF_AGENT_TYPE_GRAND_CHILD()) {
            agentIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_bot_2_n);
        }
        String botName = "CIT";
        citBot = new User(botId, botName, agentIcon);

        chatView = v.findViewById(R.id.chat_view);

        chatView.setRightBubbleColor(ContextCompat.getColor(getActivity(), R.color.color_chat_user));
        chatView.setLeftBubbleColor(ContextCompat.getColor(getActivity(), R.color.color_chat_ai));

        chatView.setSendTimeTextColor(Color.TRANSPARENT);

        chatView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.color_chat_view));

        chatView.setSendButtonColor(ContextCompat.getColor(getActivity(), R.color.lightBlue500));
        chatView.setSendIcon(R.drawable.ic_action_send);
        chatView.setOptionIcon(R.drawable.ic_action_mic);
        chatView.setOptionButtonColor(Color.WHITE);

        chatView.setRightMessageTextColor(Color.WHITE);
        chatView.setLeftMessageTextColor(Color.BLACK);

        chatView.setUsernameTextColor(Color.WHITE);
        chatView.setDateSeparatorColor(Color.WHITE);
        chatView.setInputTextHint("뭐라고 할까요?");

        chatView.setMessageMarginTop(5);
        chatView.setMessageMarginBottom(5);

        chatView.setAutoHidingKeyboard(true);

        chatView.setOnClickSendButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //new message
                if (chatView.getInputText() != null) {
                    if ("트레이닝".equals(chatView.getInputText())) {
                        mCurrentSelectedMode = ConstVariables.Companion.getUSER_SELECT_TRAINING();
                    } else if ("검사".equals(chatView.getInputText())) {
                        mCurrentSelectedMode = ConstVariables.Companion.getUSER_SELECT_TEST();
                    }
                }
                final Message message = new Message.Builder()
                        .setUser(myAccount)
                        .setRightMessage(true)
                        .setMessageText(chatView.getInputText())
                        .hideIcon(true)
                        .build();
                //Set to chat view
                chatView.send(message);
                if (chatView.getInputText().equals("제주도야")) {
                    //makeChat(null, true, mStampViewImage, -1);
                    //makeChat("정말 고맙네 자네, 다른 트레이닝도 더 받아보겠나?", false, null, -1);
                } else {
                    sendRequest(chatView.getInputText());
                }
                //Reset edit text
                chatView.setInputText("");
            }
        });

        chatView.setOnBubbleClickListener(new Message.OnBubbleClickListener() {
            @Override
            public void onClick(Message message) {
                if (message.getStatus() == IS_CLICKABLE_MSG) {
                    if (mCurrentSelectedMode == ConstVariables.Companion.getUSER_SELECT_TEST()) {
                        setEnableTestPage(true);
                    } else if (mCurrentSelectedMode == ConstVariables.Companion.getUSER_SELECT_TRAINING()) {
                        setEnableTraningPage(true);
                    }
                } else if (message.getStatus() == IS_GRAPH_VIEW) {
                    CommonDialog dialog = new CommonDialog();
                    dialog.show(getFragmentManager(), this.getClass().getSimpleName());
                } else {
                    return;
                }
            }
        });
    }

    private void openVoiceListener(ViewGroup container) {
        hideKeyboard(getActivity());
        View rootView = getLayoutInflater().inflate(R.layout.popup_voice, container, false);
        mPopupWindow = new PopupWindow(rootView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        voiceListener.startListening();

       /* final Button voice = (Button) rootView.findViewById(R.id.btn_voice);
        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vv) {
                //start listening
                if(!vv.isSelected()) {
                    vv.setSelected(true);
                    vv.setTooltipText("Listening");
                    voiceListener.startListening();
                }
                else {
                    vv.setTooltipText("Waiting");
                    String result = voiceListener.stopListening();
                    popupWindow.dismiss();
                    final Message message = new Message.Builder()
                            .setUser(myAccount)
                            .setRightMessage(true)
                            .setMessageText(result)
                            .hideIcon(true)
                            .build();
                    //Set to chat view
                    chatView.send(message);
                }
            }
        });*/
    }

    /*
     * AIRequest should have query OR event
     */
    private void sendRequest(String text) {
        Log.d(TAG, text);
        final String queryString = String.valueOf(text);
        final String eventString = null;
        final String contextString = null;

        if (TextUtils.isEmpty(queryString) && TextUtils.isEmpty(eventString)) {
            onError(new AIError(getString(R.string.non_empty_query)));
            return;
        }

        new AiTask().execute(queryString, eventString, contextString);
    }

    public class AiTask extends AsyncTask<String, Void, AIResponse> {
        private AIError aiError;

        @Override
        protected AIResponse doInBackground(final String... params) {
            final AIRequest request = new AIRequest();
            String query = params[0];
            String event = params[1];
            String context = params[2];

            if (!TextUtils.isEmpty(query)) {
                request.setQuery(query);
            }

            if (!TextUtils.isEmpty(event)) {
                request.setEvent(new AIEvent(event));
            }

            RequestExtras requestExtras = null;
            if (!TextUtils.isEmpty(context)) {
                final List<AIContext> contexts = Collections.singletonList(new AIContext(context));
                requestExtras = new RequestExtras(contexts, null);
            }

            try {
                return aiDataService.request(request, requestExtras);
            } catch (final AIServiceException e) {
                aiError = new AIError(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final AIResponse response) {
            if (response != null) {
                onResult(response);
            } else {
                onError(aiError);
            }
        }
    }


    private void onResult(final AIResponse response) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Variables
                gson.toJson(response);
                final Status status = response.getStatus();
                final Result result = response.getResult();
                final String speech = result.getFulfillment().getSpeech();
                final Metadata metadata = result.getMetadata();
                final HashMap<String, JsonElement> params = result.getParameters();

                // Logging
                Log.d(TAG, "onResult");
                Log.i(TAG, "Received success response");
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());
                Log.i(TAG, "Action: " + result.getAction());
                Log.i(TAG, "Speech: " + speech);

                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                }

                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s",
                                entry.getKey(), entry.getValue().toString()));
                    }
                }

                JSONObject job = null;
                try {
                    job = new JSONObject(speech);
                } catch (JSONException e) {
                    Log.i(TAG, "Speech was String");
                }

                if (job == null) {
                    //Response is simple String
                    String[] sentences = speech.split("\n");
                    //Update view to bot says
                    for (int i = 0; i < sentences.length; i++) {
                        Log.d(TAG, "##### onResult ##### sentences : " + sentences[i]);
                        if (sentences[i].contains("#출발")) {
                            Bitmap clickableImage = null;
                            if (mCurrentSelectedMode == ConstVariables.Companion.getUSER_SELECT_TEST()) {
                                clickableImage = mTestStartImage;
                            } else if (mCurrentSelectedMode == ConstVariables.Companion.getUSER_SELECT_TRAINING()) {
                                clickableImage = mTrainingStartImage;
                            }
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(citBot)
                                    .setRightMessage(false)
                                    //.setMessageText(sentences[i].substring(1))
                                    .setPicture(clickableImage)
                                    .setType(Message.Type.PICTURE)
                                    .setStatus(IS_CLICKABLE_MSG)
                                    .build();
                            chatView.receive(receivedMessage);
                        } else if (sentences[i].contains("#trainingupload")) {
                            Bitmap clickableImage = BitmapFactory.decodeResource(getResources(), R.drawable.training_pic);
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(citBot)
                                    .setRightMessage(false)
                                    //.setMessageText(sentences[i].substring(1))
                                    .setPicture(clickableImage)
                                    .setType(Message.Type.PICTURE)
                                    .build();
                            chatView.receive(receivedMessage);
                            sendRequest("training upload");

                        } else if (sentences[i].contains("#스탬프")) {
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(citBot)
                                    .setRightMessage(false)
                                    //.setMessageText(sentences[i].substring(1))
                                    .setPicture(mStampViewImage)
                                    .setType(Message.Type.PICTURE)
                                    .build();
                            chatView.receive(receivedMessage);
                        } else {
                            final Message receivedMessage = new Message.Builder()
                                    .setUser(citBot)
                                    .setRightMessage(false)
                                    .setMessageText(sentences[i])
                                    .build();
                            chatView.receive(receivedMessage);
                        }

                    }

                } else {
                    try {
                        /*
                        View rootView = getView();
                        final PopupWindow popupWindow = new PopupWindow(rootView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                        popupWindow.setFocusable(true);
                        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
                        */
                        ChartDrawer chartDrawer = new ChartDrawer();
                        chartDrawer.drawBarChart(job);
                    } catch (JSONException e) {

                    }
                }
            }
        });
    }

    public static Bitmap createImage(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }

    private void onError(final AIError error) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, error.toString());
            }
        });
    }


    private void initService(int type) {
        String token = FRIEND_TOKEN;
        if (type == ConstVariables.Companion.getPREF_AGENT_TYPE_FRIEND()) {
            token = FRIEND_TOKEN;
        } else if (type == ConstVariables.Companion.getPREF_AGENT_TYPE_GRAND_CHILD()) {
            token = CHILD_TOKEN;
        }
        /*
        int agent_type = PreferencesManager.INSTANCE.loadIntegerSharedPreferences(getActivity(), ConstVariables.Companion.getPREF_KEY_AGENT_TYPE());
        switch(agent_type) {
            case 1:
                token = FRIEND_TOKEN;
            case 2:
                token = CHILD_TOKEN;
            default:

        }
        */
        AIConfiguration config = new AIConfiguration(token,
                AIConfiguration.SupportedLanguages.fromLanguageTag("ko"),
                AIConfiguration.RecognitionEngine.System);
        aiDataService = new AIDataService(getActivity(), config);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_test_close:
                setEnableTestPage(false);
                break;
            case R.id.btn_test_voice:
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    if (voiceListener != null) {
                        mVoiceInputLayout.setVisibility(View.VISIBLE);
                        mAnimationDrawable.start();
                        voiceListener.startListening();
                    }
                }
                break;
            case R.id.imageview_input_voice:
                setEnableTestPage(false);
                makeChat(null, true, mGraphStartImage, IS_GRAPH_VIEW);
                break;
        }
    }

    /* Pager Area*/
    private PagerAdapter mPagerAdapter = new PagerAdapter() {
        private LayoutInflater layoutInflater;
        private ImageView closeButton;

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.layout_pager_test_step_1, container, false);

            switch (position) {
                case 0:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_1, container, false);
                    break;
                case 1:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_2, container, false);
                    mConfirmButton = view.findViewById(R.id.imageview_input_voice);
                    mConfirmButton.setOnClickListener(MainFragment.this);
                    break;
                case 2:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_3, container, false);
                    break;
                case 3:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_4, container, false);
                    break;
                case 4:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_5, container, false);
                    break;
                case 5:
                    view = layoutInflater.inflate(R.layout.layout_pager_test_step_6, container, false);
                    break;
            }
            closeButton = view.findViewById(R.id.btn_test_close);
            closeButton.setOnClickListener(MainFragment.this);
            mVoiceResultText = view.findViewById(R.id.text_input_result);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view = (View) object;
            container.removeView(view);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == ((View) o);
        }

    };
    private ViewPager.OnPageChangeListener mPagerChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {
            mProgress[i].setSelected(true);
            if ((i + 1) < mProgress.length) {
                mProgress[i + 1].setSelected(false);
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    private void setEnableTestPage(boolean flag) {
        Log.d(TAG, "##### setEnablePager #### flag : " + flag);
        if (flag) {
            mTestLayout.setVisibility(View.VISIBLE);
            mVoiceInputLayout.setVisibility(View.GONE);
            mVoiceButton.setVisibility(View.VISIBLE);
            mProgress[0].setSelected(true);
            hideKeyboard(getActivity());
        } else {
            mTestLayout.setVisibility(View.GONE);
            mVoiceInputLayout.setVisibility(View.GONE);
            mVoiceButton.setVisibility(View.GONE);
            mViewPager.setCurrentItem(0);
            voiceListener.stopListening();
            mAnimationDrawable.stop();
            int index = 0;
            for (ImageView img : mProgress) {
                mProgress[index++].setSelected(false);
            }
        }
    }

    private void makeChat(String string, boolean isClickable, Bitmap bitmap, int status) {
        Log.d(TAG, "##### makeChat #####");
        if (isClickable) {
            final Message receivedMessage = new Message.Builder()
                    .setUser(citBot)
                    .setRightMessage(false)
                    .setPicture(bitmap)
                    .setType(Message.Type.PICTURE)
                    .setStatus(status)
                    .build();
            if (chatView != null) {
                chatView.receive(receivedMessage);
            }
        } else {
            final Message receivedMessage = new Message.Builder()
                    .setUser(citBot)
                    .setRightMessage(false)
                    .setMessageText(string.toString())
                    .build();
            if (chatView != null) {
                chatView.receive(receivedMessage);
            }

        }
    }

    private void setEnableTraningPage(boolean flag) {
        Log.d(TAG, "##### setEnableTraningPage #####");
        if (flag) {
            TrainingDialog dialog = new TrainingDialog();
            dialog.show(getFragmentManager(), this.getClass().getSimpleName());
        }
    }

    private void setTraningFunction() {
        Log.d(TAG, "##### setTraningFunction mode : ##### ");
        sendRequest("트레이닝 선택");
       /* Bitmap clickableImage = null;

        if (mode == ConstVariables.Companion.getTRAINING_MODE_1()) {
            clickableImage = BitmapFactory.decodeResource(getResources(), R.drawable.img_fields_1);
        } // TBD
        makeChat(getResources().getString(R.string.string_training_step_1), false, null, -1);
        if (chatView != null) {
            final Message receivedMessage = new Message.Builder()
                    .setUser(citBot)
                    .setRightMessage(false)
                    //.setMessageText(sentences[i].substring(1))
                    .setPicture(clickableImage)
                    .setType(Message.Type.PICTURE)
                    .setStatus(IS_CLICKABLE_MSG)
                    .build();
            chatView.receive(receivedMessage);
        }
        makeChat(getResources().getString(R.string.string_training_step_2), false, null, -1);*/

    }

    private void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /*
     * Send EventBus
     * */
    @Subscribe
    public void onEvent(CommonEventBusObject obj) {
        Log.d(TAG, "##### onEvent #####");
        if (obj.getType() == ConstVariables.Companion.getEVENTBUS_INPUT_VOICE_DONE()) {
            String result = (String) obj.getValue();
            Log.d(TAG, "##### onEvent #####EVENTBUS_INPUT_VOICE_DONE result : " + result);
            if (mVoiceInputLayout != null) {
                mVoiceInputLayout.setVisibility(View.GONE);
            }
            if (mVoiceResultText != null) {
                mVoiceResultText.setText(result);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mViewPager != null) {
                        mViewPager.setCurrentItem(1);
                    }
                }
            }, 2000);

        } else if (obj.getType() == ConstVariables.Companion.getEVENTBUS_TRAINING_START()) {
            setTraningFunction();
        }
    }
}
