package com.hansung.drawingtogether.view.drawing;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hansung.drawingtogether.data.remote.model.MQTTClient;
import com.hansung.drawingtogether.data.remote.model.MyLog;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import androidx.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrawingView extends View {
    private DrawingEditor de = DrawingEditor.getInstance();
    private MQTTClient client = MQTTClient.getInstance();
    private final JSONParser parser = JSONParser.getInstance();
    private SendMqttMessage sendThread = SendMqttMessage.getInstance();

    private String topicData;

    private DrawingTool dTool = new DrawingTool();
    private Command eraserCommand = new EraseCommand();
    private Command selectCommand = new SelectCommand();

    private boolean isIntercept = false;
    //private int currentDrawAction = MotionEvent.ACTION_UP;
    private float canvasWidth;
    private float canvasHeight;
    private DrawingComponent dComponent;
    private Stroke stroke = new Stroke();
    private Rect rect = new Rect();
    private Oval oval = new Oval();

    public DrawingView(Context context) {
        super(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        MyLog.e("DrawingView", "call onSizeChanged");

        topicData = client.getTopic_data();
        canvasWidth = w;
        canvasHeight = h;

        if(de.getDrawingBitmap() == null) {
            de.setDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setLastDrawingBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setPreSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setPostSelectedComponentsBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
            de.setSelectedComponentBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));

            de.setBackCanvas(new Canvas(de.getDrawingBitmap()));

            //sendMqttMessageThread.start();
        }
        if(de.getDrawingBoardArray() == null) {
            de.initDrawingBoardArray(w, h);
        }
        if(client.isMaster()) {
            MyLog.i("mqtt", "progressDialog dismiss");
            client.getProgressDialog().dismiss();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(de.getDrawingBitmap(), 0, 0, null);
        canvas.drawBitmap(de.getSelectedComponentBitmap(), 0, 0, null);
        //Log.i("drawing", "onDraw");
        //this.invalidate();
    }

    long drawCnt = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //MyLog.i("drawing", "drawing view isIntercept = " + isIntercept + ", de isIntercept = " + de.isIntercept());

        if(!de.isIntercept()) this.isIntercept = false;

        if(isIntercept || event.getAction() == MotionEvent.ACTION_DOWN && de.isIntercept()) {
            MyLog.i("drawing", "intercept drawing view touch");
            return false; // 부모뷰에게 터치 이벤트가 넘어가도록 [ intercept = true, set MQTTClient.java ]
        } else {
            this.getParent().requestDisallowInterceptTouchEvent(true); // 부모뷰에게 터치 이벤트 뺏기지 않도록
        }
        //this.getParent().requestDisallowInterceptTouchEvent(true);

        setEditorAttribute();
        if(de.getCurrentMode() != Mode.SELECT) {    //todo - 나중에 다른 버튼 클릭 시로 수정
            isSelected = false;
            de.clearSelectedBitmap();
            invalidate();
        }
        switch (de.getCurrentMode()) {
            case DRAW:
                setDrawingComponentType();
                moveCnt++;
                drawCnt++;
                return onTouchDrawMode(event);

            case ERASE:
                return onTouchEraseMode(event);

            case SELECT:
                return onTouchSelectMode(event);

            case GROUP:
                break;
            case WARP:
                return onTouchWarpMode(event);
        }
        return super.onTouchEvent(event);
    }

    public void setEditorAttribute() {
        de.setCurrentType(de.getCurrentType());
        de.setUsername(de.getMyUsername());
        de.setDrawnCanvasWidth(canvasWidth);
        de.setDrawnCanvasHeight(canvasHeight);
        de.setMyCanvasWidth(canvasWidth);
        de.setMyCanvasHeight(canvasHeight);

        /*
        Random random = new Random();   //fixme nayeon
        int color = Color.rgb(random.nextInt(128) +  128, random.nextInt(128) +  128, random.nextInt(128) +  128);
        de.setStrokeColor(color);
        de.setFillColor(color);
        */
    }

    public void setDrawingComponentType() {
        switch(de.getCurrentType()) {
            case STROKE:
                dComponent = stroke;
                break;
            case RECT:
                dComponent = rect;
                de.setDrawingShape(true);
                break;
            case OVAL:
                dComponent = oval;
                de.setDrawingShape(true);
                break;
        }
    }

    public void setComponentAttribute(DrawingComponent dComponent) {
        dComponent.setId(de.componentIdCounter());  //id 자동 증가
        dComponent.setUsername(de.getUsername());
        dComponent.setUsersComponentId(dComponent.username + "-" + dComponent.id);
        dComponent.setType(de.getCurrentType());
        dComponent.setFillColor(de.getFillColor());
        dComponent.setStrokeColor(de.getStrokeColor());
        dComponent.setStrokeAlpha(de.getStrokeAlpha());
        dComponent.setFillAlpha(de.getFillAlpha());
        dComponent.setStrokeWidth(de.getStrokeWidth());
        dComponent.setDrawnCanvasWidth(de.getDrawnCanvasWidth());
        dComponent.setDrawnCanvasHeight(de.getDrawnCanvasHeight());
        dComponent.calculateRatio(de.getMyCanvasWidth(), de.getMyCanvasHeight());   //화면 비율 계산
    }

    public void addPointAndDraw(DrawingComponent dComponent, Point point) {
        dComponent.setPreSize(dComponent.getPoints().size());
        dComponent.addPoint(point);
        dComponent.setBeginPoint(dComponent.getPoints().get(0));
        dComponent.setEndPoint(point);

        dComponent.draw(de.getBackCanvas());

        if(dComponent.getType() == ComponentType.STROKE) {
            Canvas canvas = new Canvas(de.getLastDrawingBitmap());
            dComponent.draw(canvas);
        } /*else {
            de.updateCurrentShapes(dComponent);
        }*/
    }

    public void initDrawingComponent() {
        switch(de.getCurrentType()) {
            case STROKE:
                stroke = new Stroke();
                break;
            case RECT:
                rect = new Rect();
                break;
            case OVAL:
                oval = new Oval();
                break;
        }
    }

    public void sendModeMqttMessage() {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode());
        client.publish(topicData, parser.jsonWrite(messageFormat));
    }

    /*public void sendDrawMqttMessage(final int action) {
        MqttMessageFormat messageFormat = new MqttMessageFormat(de.getMyUsername(), de.getCurrentMode(), de.getCurrentType(), dComponent, action);
        client.publish(topicData,  parser.jsonWrite(messageFormat));
    }*/

    /*private BlockingQueue<MqttMessageFormat> queue = new ArrayBlockingQueue<>(1000);    //Linked, Array 두개의 차이 알아보기
    private final Object lock = new Object();
    private SendMqttMessageThread sendMqttMessageThread = new SendMqttMessageThread();
    private boolean isWait = false;
    private int cnt = 0;
    private int putCnt = 0;
    private ArrayList<MqttMessageFormat> messages = new ArrayList<>();

    public void putMqttMessage(MqttMessageFormat messageFormat) {    //producer  //queue 가 꽉차있으면 wait, 아니면 put 하는 thread
        try {
            queue.put(messageFormat);
            putCnt++;
            Log.i("sendThread", "offer success " + putCnt + ", size() = " + queue.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/

    /*class SendMqttMessageThread extends Thread {    //consumer  //queue 가 비어있을때까지 publish 하는 thread
        @Override
        public void run() {
            try {
                while(true) {
                    Log.i("sendThread", "draw touch count = " + drawCnt);
                    try {
                        Log.i("sendThread", "before publish");
                        MqttMessageFormat messageFormat = queue.take();
                        Log.i("sendThread", Thread.activeCount() + ", ");
                        client.publish(topicData, parser.jsonWrite(*//*queue.take()*//*messageFormat));
                        //Thread.sleep(10);

                        cnt++;
                        Log.i("sendThread", "poll success " + cnt + ", size() = " + queue.size());

                    } catch (ConcurrentModificationException e) {
                        Log.i("sendThread", "*** ConcurrentModificationException ***");
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }*/

    public Integer updateDrawingComponentId(DrawingComponent dComponent) {
        try {
            DrawingComponent upComponent = de.findCurrentComponent(dComponent.getUsersComponentId());

            //MyLog.i("drawing", "upComponent: id=" + upComponent.getId() + ", endPoint=" + upComponent.getEndPoint().toString());

            dComponent.setId(upComponent.getId());
            return dComponent.getId();
        } catch(NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void doInDrawActionUp(DrawingComponent dComponent) {
        initDrawingComponent(); // 드로잉 컴포넌트 객체 생성

        de.splitPoints(dComponent, de.getMyCanvasWidth(), de.getMyCanvasHeight());
        //de.removeCurrentComponents(dComponent.getId());
        de.removeCurrentComponents(dComponent.getUsersComponentId());
        de.removeCurrentShapes(dComponent.getUsersComponentId());
        de.addDrawingComponents(dComponent);
        MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

        de.addHistory(new DrawingItem(de.getCurrentMode(), dComponent/*, de.getDrawingBitmap()*/)); // 드로잉 컴포넌트가 생성되면 History 에 저장

        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        MyLog.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + dComponent.getId());
        if(dComponent.getType() != ComponentType.STROKE) { // 도형이 그려졌다면 lastDrawingBitmap 에 drawingBitmap 내용 복사
            if(de.getCurrentShapes().size() == 0)
                de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
            else {
                Canvas canvas = new Canvas(de.getLastDrawingBitmap());
                dComponent.draw(canvas);
            }
        }

        de.clearUndoArray();

        if(de.isIntercept()) this.isIntercept = true;

        de.setDrawingShape(false);
    }

    /*public void InterceptTouchEventAndDoActionUp() {
        if(currentDrawAction == MotionEvent.ACTION_UP) {
            return;
        }
        isIntercept = true;
        Log.i("drawing", "intercept touch event and do action up");

        this.getParent().requestDisallowInterceptTouchEvent(false);
        sendDrawMqttMessage(MotionEvent.ACTION_UP);

        addPointAndDraw(dComponent, dComponent.getEndPoint());
        updateDrawingComponentId(dComponent);
        doInDrawActionUp(dComponent);
    }*/

    boolean isExit = false;
    int moveCnt = 0;
    public boolean onTouchDrawMode(MotionEvent event/*, DrawingComponent dComponent*/) {
        Point point;
        //currentDrawAction = event.getAction();
        //de.setCurrentDrawAction(event.getAction());

        if(isExit && event.getAction() != MotionEvent.ACTION_DOWN) {
            MyLog.i("mqtt", "isExit1 = " + isExit);
            return true;
        }

        // 터치가 DrawingView 밖으로 나갔을 때
        if(event.getX()-5 < 0 || event.getY()-5 < 0 || de.getDrawnCanvasWidth()-5 < event.getX() || de.getDrawnCanvasHeight()-5 < event.getY()) {   //fixme 반응이 느려서 임시로 -5
            //currentDrawAction = MotionEvent.ACTION_UP;
            //de.setCurrentDrawAction(MotionEvent.ACTION_UP);

            MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());
            MyLog.i("drawing", "exit");

            point = dComponent.getEndPoint();
            addPointAndDraw(dComponent, point);

            //publish
            //sendDrawMqttMessage(MotionEvent.ACTION_UP);

            sendThread.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), updateDrawingComponentId(dComponent), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, MotionEvent.ACTION_UP));
            //putMqttMessage(MotionEvent.ACTION_UP);

            updateDrawingComponentId(dComponent);
            doInDrawActionUp(dComponent);

            isExit = true;
            MyLog.i("mqtt", "isExit2 = " + isExit);
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isExit = false;
                MyLog.i("mqtt", "isExit3 = " + isExit);

                /*de.addCurrentComponents(dComponent);
                Log.i("drawing", "currentComponents.size() = " + de.getCurrentComponents().size());
                */
                //de.addCurrentShapes(dComponent);

                setComponentAttribute(dComponent);
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                //sendDrawMqttMessage(event.getAction());

                //down에서는 DrawingComponent 자체를 보내고, move, up에서는 추가된 점에 관한 정보만 보낸다. fixme minj

                sendThread.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), dComponent, event.getAction()));
                //putMqttMessage(event.getAction());

                return true;

            case MotionEvent.ACTION_MOVE:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);

                //publish
                //sendDrawMqttMessage(event.getAction());

                //if(moveCnt == 5) {
                    sendThread.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), updateDrawingComponentId(dComponent), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, event.getAction()));
                    //moveCnt = -1;
                //}

                //putMqttMessage(event.getAction());

                return true;

            case MotionEvent.ACTION_UP:
                point = new Point((int)event.getX(), (int)event.getY());
                addPointAndDraw(dComponent, point);
                MyLog.i("drawing", "id=" + dComponent.getId() + ", username=" + dComponent.getUsername() + ", begin=" + dComponent.getBeginPoint() + ", end=" + dComponent.getEndPoint());


                updateDrawingComponentId(dComponent);
                doInDrawActionUp(dComponent);

                //publish
                //sendDrawMqttMessage(event.getAction());

                //putMqttMessage(event.getAction());
                sendThread.putMqttMessage(new MqttMessageFormat(de.getMyUsername(), updateDrawingComponentId(dComponent), dComponent.getUsersComponentId(), de.getCurrentMode(), de.getCurrentType(), point, event.getAction()));

                return true;
            default:
                MyLog.i("drawing", "action = " + MotionEvent.actionToString(event.getAction()));
        }
        return true;
    }

    public boolean onTouchEraseMode(MotionEvent event) {

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                dTool.setCommand(eraserCommand);

                Point point = new Point((int)event.getX(), (int)event.getY());
                dTool.doCommand(point);

                return true;
        }
        return true;
    }

    Point selectPrePoint;
    Point selectPostPoint;
    boolean isSelected = false;
    int selectMoveCount = 0;
    Point selectDownPoint;
    public boolean onTouchSelectMode(MotionEvent event) {
        dTool.setCommand(selectCommand);

        if(!isSelected) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selectPrePoint = new Point((int) event.getX(), (int) event.getY());
                case MotionEvent.ACTION_MOVE:
                    selectPostPoint = new Point((int) event.getX(), (int) event.getY());
                    if (!selectPrePoint.equals(selectPostPoint)) {
                        selectMoveCount++;
                        MyLog.i("drawing", "move pre=" + selectPrePoint.toString() + ", post=" + selectPostPoint.toString() + ", " + selectMoveCount);

                        selectPrePoint = selectPostPoint;
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    dTool.doCommand(selectPostPoint);
                    int selectedComponentId = dTool.getIds().get(0);
                    if (selectedComponentId != -1 && selectMoveCount <= 7) {    // 제스처로 할 지 고민
                        isSelected = true;

                        de.setSelectedComponent(de.findDrawingComponentById(selectedComponentId));
                        de.setPreSelectedComponents(selectedComponentId);
                        de.setPostSelectedComponents(selectedComponentId);

                        de.clearSelectedBitmap();
                        de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                        invalidate();

                        //todo publish - 다른 사람들 셀렉트 못하게
                        MyLog.i("drawing", "selected id=" + selectedComponentId);
                    } else {
                        isSelected = false;
                        de.clearSelectedBitmap();
                        invalidate();

                        //todo publish - 다른 사람들 셀렉트 가능 --> 모드 바뀔 때 추가로 메시지 전송 필요
                        MyLog.i("drawing", "not selected=" + selectedComponentId);
                    }
                    selectMoveCount = 0;
                    return true;
            }
            return true;
        } else {
            int moveX, moveY;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    MyLog.i("drawing", "selected down");
                    selectDownPoint = new Point((int)event.getX(), (int)event.getY());
                    if(!de.isContainsSelectedComponent(selectDownPoint)) {
                        isSelected = false;
                        MyLog.i("drawing", "selected false");
                        return true;
                    }

                    de.drawAllPreSelectedComponents();
                    de.drawAllPostSelectedComponents();
                    de.drawSelectedComponent();
                    de.drawSelectedBitmaps();
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    invalidate();

                    //todo publish - selected down
                    MyLog.i("drawing", "selected true");
                    return true;

                case MotionEvent.ACTION_MOVE:
                    MyLog.i("drawing", "selected move");
                    moveX = (int)event.getX() - selectDownPoint.x;
                    moveY = (int)event.getY() - selectDownPoint.y;

                    Point datumPoint = de.getSelectedComponent().getDatumPoint();
                    int width = de.getSelectedComponent().getWidth();
                    int height = de.getSelectedComponent().getHeight();

                    if((datumPoint.x-10 < 0 && moveX < 0) || (datumPoint.y-10 < 0 && moveY < 0) || (datumPoint.y+height+10 > de.getDrawnCanvasHeight() && moveY > 0) || (datumPoint.x+width+10 > de.getDrawnCanvasWidth() && moveX > 0))
                        return true;

                    selectDownPoint = new Point((int)event.getX(), (int)event.getY());

                    de.moveSelectedComponent(moveX, moveY);
                    de.clearSelectedBitmap();
                    de.getSelectedComponent().drawComponent(de.getSelectedCanvas());
                    de.drawSelectedComponentBorder(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    invalidate();

                    //todo publish - selected move
                    return true;

                case MotionEvent.ACTION_UP:
                    MyLog.i("drawing", "selected up");

                    de.updateDrawingBitmap();
                    de.updateSelectedComponent(de.getSelectedComponent(), de.getMyCanvasWidth(), de.getMyCanvasHeight());
                    de.updateDrawingComponents(de.getSelectedComponent());
                    MyLog.i("drawing", "drawingComponents.size() = " + de.getDrawingComponents().size());

                    //de.addHistory(new DrawingItem(de.getCurrentMode(), de.getSelectedComponent())); //todo
                    //Log.i("drawing", "history.size()=" + de.getHistory().size() + ", id=" + de.getSelectedComponent().getId());

                    de.setLastDrawingBitmap(de.getDrawingBitmap().copy(de.getDrawingBitmap().getConfig(), true));
                    de.clearUndoArray();
                    invalidate();

                    //if(de.isIntercept()) this.isIntercept = true;   //todo - DRAW mode 외에도 중간자 join 시 intercept 처리

                    //todo publish - selected up
                    return true;
            }
            return true;
        }
    }

    public boolean onTouchWarpMode(MotionEvent event) {
        return false;
    }

    public void clear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("화면 초기화").setMessage("모든 그리기 내용이 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR);
                sendModeMqttMessage();
                de.clearDrawingComponents();
                de.clearTexts();
                de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);
                de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);
                invalidate();

                MyLog.i("drawing", "history.size()=" + de.getHistory().size());
                MyLog.i("drawing", "clear");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void clearBackgroundImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(de.getDrawingFragment().getActivity());
        builder.setTitle("배경 초기화").setMessage("배경 이미지가 삭제됩니다.\n그래도 지우시겠습니까?");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                de.setCurrentMode(Mode.CLEAR_BACKGROUND_IMAGE);
                sendModeMqttMessage();
                de.setBackgroundImage(null);
                de.clearBackgroundImage();

                MyLog.i("drawing", "clear background image");
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MyLog.i("drawing", "canceled");
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void undo() {
        Mode preMode = de.getCurrentMode();
        de.setCurrentMode(Mode.UNDO);
        sendModeMqttMessage();
        de.undo();

        if(de.getUndoArray().size() == 1)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(true);

        if(de.getHistory().size() == 0)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(false);

        invalidate();
        de.setCurrentMode(preMode);
    }

    public void redo() {
        Mode preMode = de.getCurrentMode();
        de.setCurrentMode(Mode.REDO);
        sendModeMqttMessage();
        de.redo();
        if(de.getHistory().size() == 1)
            de.getDrawingFragment().getBinding().undoBtn.setEnabled(true);

        if(de.getUndoArray().size() == 0)
            de.getDrawingFragment().getBinding().redoBtn.setEnabled(false);

        invalidate();
        de.setCurrentMode(preMode);
    }
}
