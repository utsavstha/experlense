package fr.pchab.androidrtc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import fr.pchab.webrtcclient.WebRtcClient;
import fr.pchab.webrtcclient.PeerConnectionParameters;

public class RtcActivity extends Activity implements WebRtcClient.RtcListener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;


    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView glSurfaceView;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient webRtcClient;
    private String serverAddress;
    private String technicianId;
    private String clientId;

    private boolean callReady = false;

    Button pauseBtn;
    boolean isPaused = false;

    DrawingView drawingView = null;

    boolean isTechnician = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.main);
        serverAddress = "http://" + getResources().getString(R.string.host) + ":" + getResources().getString(R.string.port) + "/";
        technicianId = getIntent().getStringExtra("receiver");
        if(technicianId ==null) technicianId ="";
        isTechnician = technicianId.isEmpty();

        Toast.makeText(this, "Expert: "+isTechnician, Toast.LENGTH_SHORT).show();
        if(!isTechnician){
            Toast.makeText(this, "Connecting to "+ technicianId, Toast.LENGTH_SHORT).show();
        }

        initScreenView(); //make the camera readers/displayers ready
        initRenders();//makes the screen ready

        initDrawingView();


    }

    void initRenders(){
        // local and remote render

        if(isTechnician){
            remoteRender = VideoRendererGui.create(
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        }else{
            localRender = VideoRendererGui.create(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                    LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        }
    }

    void initScreenView(){
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glview_call);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setKeepScreenOn(true);
        VideoRendererGui.setView(glSurfaceView, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    void initDrawingView(){
        pauseBtn = (Button) findViewById(R.id.pauseBtn);
        drawingView = (DrawingView) findViewById(R.id.drawingView);

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    //resume
                    onResume();
                    drawingView.clear();

                    if (isTechnician) {
                        drawingView.setCanDraw(false);
                    }
                    isPaused = false;
                    pauseBtn.setText("Pause");

                    try {
                       // webRtcClient.sendMessage(clientId,"resume_screen", new JSONObject());
                        webRtcClient.sendMessage(clientId,"draw_clear",new JSONObject());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    //pause the stream
                    onPause();

                    if (isTechnician) {
                        try {
                            webRtcClient.sendMessage(clientId, "pause_screen", new JSONObject());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        drawingView.setCanDraw(true);
                        Toast.makeText(RtcActivity.this, "You can draw over the screen now.", Toast.LENGTH_SHORT).show();
                    }

                    isPaused = true;
                    pauseBtn.setText("Resume");
                }
            }
        });

        if(isTechnician){
            pauseBtn.setVisibility(View.VISIBLE);
            drawingView.setCanDraw(false);
            drawingView.setPaintColor(Color.YELLOW);

            drawingView.setThirdEyeEventListener(new ThirdEyeDrawEvent.ThirdEyeEventListener() {
                @Override
                public void onThirdEyeEvent(final ThirdEyeDrawEvent event) {
                    Log.d("###", "Third eye draw event");
                    if(clientId==null || clientId.isEmpty()){
                        Log.d("###","Client not found!");
                        return;
                    }

                    try {

                        webRtcClient.sendMessage(clientId, "gesture", event.toJson() );
                        Log.d("###","Sent gesture to "+clientId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("###","Could not send gesture");
                    }
                }
            });
        }else{
            //client side
            pauseBtn.setVisibility(View.GONE);

            drawingView.setCanDraw(true);
            drawingView.setPaintColor(Color.GREEN);
            drawingView.setVisibility(View.VISIBLE);
        }
    }


    void initDrawingGestureListener(){
        //used by the client to receive drawing gestures from the technician
        webRtcClient.messageHandler.addCustomCommand("gesture", new WebRtcClient.Command() {
            @Override
            public void execute(String peerId, final JSONObject payload) throws JSONException {
                Log.d("###", "Gesture received");

                RtcActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final ThirdEyeDrawEvent event = ThirdEyeDrawEvent.fromJson(payload);

                            drawingView.implementThirdEyeEvent(event);
                            Log.d("###", event.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("###", "Could not parse thirdeye event " + e.getMessage());
                        }
                    }
                });
            }
        });

        webRtcClient.messageHandler.addCustomCommand("draw_clear", new WebRtcClient.Command() {
            @Override
            public void execute(String peerId, JSONObject payload) throws JSONException {
                Log.d("##","Clear gesture order received");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onResume();
                        drawingView.clear();
                    }
                });
            }
        });

        webRtcClient.messageHandler.addCustomCommand("pause_screen", new WebRtcClient.Command() {
            @Override
            public void execute(String peerId, JSONObject payload) throws JSONException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onPause();
                    }
                });
            }
        });

    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        webRtcClient = new WebRtcClient(this, serverAddress, params, VideoRendererGui.getEGLContext());


        if(!isTechnician)
            initDrawingGestureListener();
        else{
            webRtcClient.addPeerConnectedListener(new WebRtcClient.Command() {
                @Override
                public void execute(String peerId, JSONObject payload) throws JSONException {
                    Log.d("###1", "Client connected: " + peerId);
                    clientId = peerId;
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        if(webRtcClient != null) {
            webRtcClient.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        if(webRtcClient != null) {
            webRtcClient.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if(webRtcClient != null) {
            webRtcClient.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        callReady = true;
        Log.d("###","CALL READY: "+technicianId+" vs "+callId);

        if (!isTechnician) {
            try {
                answer(technicianId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("###","Calling "+callId);
            call(callId);
        }
    }

    public void answer(String callerId) throws JSONException {

        Log.d("###", "Answering " + callerId);


        webRtcClient.sendMessage(callerId, "init", null);
        startCam();
    }

    public void call(final String callId) {
        Log.d("###", "Call " + callId);
        RtcActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startCam();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VIDEO_CALL_SENT) {
            startCam();
        }
    }

    public void startCam() {
        // Camera settings
        webRtcClient.start(isTechnician?"Technician":"Client");
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        if(isTechnician)return;

        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));

        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        if(!isTechnician)return;
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));

        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType);


    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        if(isTechnician)return;
        if(!isTechnician)return;

        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }
}