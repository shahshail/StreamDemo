package com.streamunlimited.streamsdkdemo.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;

/**
 * Created by sebastian on 8/12/14.
 */
public class KnobWidgetView extends RelativeLayout {

    private Matrix _rotatorMatrix;
    private Matrix _arrowMatrix;

    // Image Resource vars
    private int _rotatorImg;
    private int _innerBackgroundImg;
    private int _innerArrowImg;

    // General settings vars
    private boolean _showText;
    private int _minValue;
    private int _maxValue;
    private int _turnsToMax;
    private double _currVal = 0;

    // Arrow vars
    private int _arrowStartAngle;
    private int _arrowEndAngle;
    private int _arrowCenterYDelta;
    private int _arrowBottomYOffset;
    private int _imgArrowHeight = 0;
    private int _innerBackgroundPadding;
    private int _ivArrowWidth = 0;
    private int _ivArrowHeight = 0;
    private float _arrowCenterY;
    private double _arrowCurrAngle = 90;

    // Rotator vars
    private int _rotatorWidth = 0;
    private int _rotatorHeight = 0;

    // View element holders
    private TextView _txtValue;
    private ImageView _ivRotator;
    private ImageView _ivInnerBackground;
    private ImageView _ivInnerArrow;

    private onKnobEventListener _listener = null;

    public interface onKnobEventListener {
        void onKnobTouchStart();

        void onKnobTouchEnd();

        void onKnobValueChanged(double value);

        void onKnobLongClick();
    }

    public KnobWidgetView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.KnobWidgetView, 0, 0);

        try {
            _showText = a.getBoolean(R.styleable.KnobWidgetView_showText, false);
            _rotatorImg = a.getResourceId(R.styleable.KnobWidgetView_rotatorImg, 0);
            _innerBackgroundImg = a.getResourceId(R.styleable.KnobWidgetView_innerBackgroundImg, 0);
            _innerArrowImg = a.getResourceId(R.styleable.KnobWidgetView_innerArrowImg, 0);
            _turnsToMax = a.getInteger(R.styleable.KnobWidgetView_turnsToMax, 1);
            _minValue = a.getInteger(R.styleable.KnobWidgetView_minValue, 0);
            _maxValue = a.getInteger(R.styleable.KnobWidgetView_maxValue, 100);
            _arrowStartAngle = getDegreesFromMinutes(a.getInteger(R.styleable.KnobWidgetView_arrowStartAngleInMinutes, 45));
            _arrowEndAngle = getDegreesFromMinutes(a.getInteger(R.styleable.KnobWidgetView_arrowEndAngleInMinutes, 15));
            _innerBackgroundPadding = a.getInteger(R.styleable.KnobWidgetView_innerBackgroundPadding, 0);
            _arrowCenterYDelta = a.getInteger(R.styleable.KnobWidgetView_arrowCenterYDelta, 0);
            _arrowBottomYOffset = a.getInteger(R.styleable.KnobWidgetView_arrowBottomYOffset, 0);
        } finally {
            a.recycle();
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.knob_widget, this, true);

        if (_rotatorImg != 0) {
            _rotatorMatrix = new Matrix();
            _ivRotator = (ImageView) findViewById(R.id.rotatorImg);
            _ivRotator.setOnTouchListener(new OnKnobTouchListener());
            _ivRotator.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    _ivRotator.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    _rotatorHeight = _ivRotator.getMeasuredHeight();
                    _rotatorWidth = _ivRotator.getMeasuredWidth();

                    // resize
                    Bitmap imageOriginal = BitmapFactory.decodeResource(context.getResources(), _rotatorImg);
                    Matrix resize = new Matrix();
                    resize.postScale((float) Math.min(_rotatorWidth, _rotatorHeight) / (float) imageOriginal.getWidth(), (float) Math.min(_rotatorWidth, _rotatorHeight) / (float) imageOriginal.getHeight());
                    Bitmap imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                    // translate to the image view's center
                    float translateX = _rotatorWidth / 2 - imageScaled.getWidth() / 2;
                    float translateY = _rotatorHeight / 2 - imageScaled.getHeight() / 2;
                    _rotatorMatrix.postTranslate(translateX, translateY);

                    _ivRotator.setImageBitmap(imageScaled);
                    _ivRotator.setImageMatrix(_rotatorMatrix);
                }
            });
        }
        if (_innerBackgroundImg != 0) {
            _ivInnerBackground = (ImageView) findViewById(R.id.innerBackgroundImg);
            _ivInnerBackground.setImageResource(_innerBackgroundImg);
            _ivInnerBackground.setOnLongClickListener(new OnKnobLongClickListener());
            _ivInnerBackground.setVisibility(VISIBLE);
        }
        if (_innerArrowImg != 0) {
            _arrowMatrix = new Matrix();
            _ivInnerArrow = (ImageView) findViewById(R.id.innerArrowImg);
            //_ivInnerArrow.setImageResource(_innerArrowImg);
            _ivInnerArrow.setVisibility(VISIBLE);
            _ivInnerArrow.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    _ivInnerArrow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    _ivArrowHeight = _ivInnerArrow.getMeasuredHeight();
                    _ivArrowWidth = _ivInnerArrow.getMeasuredWidth();
                    _imgArrowHeight = (int) ((Math.min(_ivArrowWidth, _ivArrowHeight) * ((100f - _innerBackgroundPadding) / 100f) / 2f) / (100f - _arrowBottomYOffset) * 100f);
                    _arrowCenterY = _ivArrowHeight / 2f + (_arrowBottomYOffset - _arrowCenterYDelta) / 100f * _imgArrowHeight;

                    // resize
                    Bitmap imageOriginal = BitmapFactory.decodeResource(context.getResources(), _innerArrowImg);
                    Matrix resize = new Matrix();
                    float scale = (float) _imgArrowHeight / (float) imageOriginal.getHeight();
                    resize.postScale(scale, scale);
                    Bitmap imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                    // translate to the inner background center + offset
                    float translateX = _ivArrowWidth / 2 - imageScaled.getWidth() / 2;
                    float translateY = _arrowCenterY - imageScaled.getHeight() + _arrowCenterYDelta / 100f * _imgArrowHeight;
                    _arrowMatrix.postTranslate(translateX, translateY);

                    _ivInnerArrow.setImageBitmap(imageScaled);
                    _ivInnerArrow.setImageMatrix(_arrowMatrix);
                    _ivInnerArrow.bringToFront();

                    updateArrow();
                }
            });
        }
        if (_showText) {
            _txtValue = (TextView) findViewById(R.id.txt_value);
            updateValueText();
            _txtValue.setVisibility(VISIBLE);
        }
    }

    public void setKnobEventListener(onKnobEventListener listener) {
        this._listener = listener;
    }

    public int getMinValue() {
        return _minValue;
    }

    public void setMinValue(int minValue) {
        this._minValue = minValue;
    }

    public int getMaxValue() {
        return _maxValue;
    }

    public void setMaxValue(int maxValue) {
        this._maxValue = maxValue;
    }

    public double getCurrVal() {
        return _currVal;
    }

    public void setCurrVal(double value) {
        this._currVal = value;
        updateValueText();
        updateArrow();
    }

    public int getTurnsToMax() {
        return _turnsToMax;
    }

    public void setTurnsToMax(int turnsToMax) {
        this._turnsToMax = turnsToMax;
    }

    public boolean isShowText() {
        return _showText;
    }

    public void setShowText(boolean showText) {
        this._showText = showText;
        invalidate();
        requestLayout();
    }

    public int getRotatorImg() {
        return _rotatorImg;
    }

    public void setRotatorImg(int rotatorImg) {
        this._rotatorImg = rotatorImg;
        invalidate();
        requestLayout();
    }

    public int getInnerBackgroundImg() {
        return _innerBackgroundImg;
    }

    public void setInnerBackgroundImg(int innerBackgroundImg) {
        this._innerBackgroundImg = innerBackgroundImg;
        invalidate();
        requestLayout();
    }

    public int getInnerArrowImg() {
        return _innerArrowImg;
    }

    public void setInnerArrowImg(int innerArrowImg) {
        this._innerArrowImg = innerArrowImg;
        invalidate();
        requestLayout();
    }

    /**
     * @return The angle of the unit circle with the image view's center
     */
    private double getRotatorAngle(double xTouch, double yTouch) {
        double x = xTouch - (this.getWidth() / 2d);
        double y = (this.getHeight() / 2d) - yTouch;

        return getAngle(x, y);
    }

    private double getAngle(double x, double y) {
        switch (getQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 2:
                return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 3:
                return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                return 0;
        }
    }

    /**
     * @return The selected quadrant.
     */
    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    /**
     * @return The angle of the arrow for given value
     */
    private double getArrowAngleFromValue(double value) {
        int range = _maxValue - _minValue;
        double angleMinMax = (_arrowStartAngle - _arrowEndAngle) % 360;
        double anglePerUnit = angleMinMax / range;
        double angleCenter = 0;
        double centerToHead = Math.min(_ivArrowWidth, _ivArrowHeight) * ((100f - _innerBackgroundPadding) / 100f) / 2f;

        if (value > _maxValue) {
            value = _maxValue;
        } else if (value < _minValue) {
            value = _minValue;
        }

        double valueDiff = value - _minValue;

        // calculate angle from center
        angleCenter = (_arrowStartAngle - (valueDiff * anglePerUnit)) % 360;

        // now calculate arrow angle from arrow tail
        double x = Math.cos(angleCenter * Math.PI / 180) * centerToHead;
        double y = Math.sin(angleCenter * Math.PI / 180) * centerToHead + (_arrowBottomYOffset - _arrowCenterYDelta) / 100f * _imgArrowHeight;

        return getAngle(x, y);
    }

    /**
     * @return Convert minutes (on a clock) to degrees
     */
    private int getDegreesFromMinutes(int min) {
        // rotate clock wise by 90 degrees and calculate degrees
        int minAngle = (min - 15) % 60;
        // Java modulo implementation returns negative values for negativ input, so correct this
        if (minAngle < 0) {
            minAngle += 60;
        }
        minAngle = minAngle * 6;

        if (minAngle == 0) {
            return 0;
        } else {
            return 360 - minAngle;
        }
    }

    /**
     * Rotate the knob.
     *
     * @param degrees The degrees, the dialer should get rotated.
     */
    private void rotateKnob(float degrees) {
        _rotatorMatrix.postRotate(degrees, _rotatorWidth / 2, _rotatorHeight / 2);
        _ivRotator.setImageMatrix(_rotatorMatrix);
    }

    /**
     * Rotate the arrow.
     *
     * @param degrees The degrees, the dialer should get rotated.
     */
    private void rotateArrow(float degrees) {
        _arrowMatrix.postRotate(degrees, _ivArrowWidth / 2, _arrowCenterY);
        _ivInnerArrow.setImageMatrix(_arrowMatrix);
    }

    /**
     * Update the arrow position
     */
    private void updateArrow() {
        // check if arrow is enabled and already shown
        if (_innerArrowImg != 0 && _ivArrowHeight != 0) {
            double arrowAngle = getArrowAngleFromValue(_currVal);
            rotateArrow((float) (_arrowCurrAngle - arrowAngle));
            _arrowCurrAngle = arrowAngle;
        }
    }


    /**
     * Update the value text field
     */
    private void updateValueText() {
        if (_showText && _txtValue != null) {
            _txtValue.setText(String.valueOf((int) _currVal));
        }
    }

    /**
     * Simple implementation of an {@link OnTouchListener} for registering touch events.
     */
    private class OnKnobTouchListener implements OnTouchListener {

        private boolean rotatorTouched = false;
        private double startAngle;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    double x = event.getX() - (_rotatorWidth / 2d);
                    double y = (_rotatorHeight / 2d) - event.getY();

                    int size = Math.min(_rotatorWidth, _rotatorHeight);
                    float outerBound = size / 2;
                    float innerBound = outerBound - Math.min(_ivArrowWidth, _ivArrowHeight) * ((_innerBackgroundPadding / 100f) / 2f);

                    double hyp = Math.hypot(x, y);

                    if (hyp <= outerBound && hyp >= innerBound) {
                        rotatorTouched = true;
                        startAngle = getRotatorAngle(event.getX(), event.getY());

                        if (_listener != null) {
                            _listener.onKnobTouchStart();
                        }
                    } else {
                        return false;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    // rotate only if rotator was touched
                    if (rotatorTouched) {
                        double currentAngle = getRotatorAngle(event.getX(), event.getY());
                        double degreeMaxTurns = 360 * _turnsToMax;
                        double range = (_maxValue - _minValue);

                        /*
                         * Handle a complete turn of the knob -> angle jumps from 359 to 0
                         * tolerance of 50 seems to be a good value
                         */
                        int tolerance = 50;
                        if (startAngle > (360 - tolerance) && currentAngle < tolerance) {
                            startAngle = startAngle - 360;
                        } else if (startAngle < 50 && currentAngle > 310) {
                            startAngle = startAngle + 360;
                        }

                        // calculate the current value and check if it is not out of bounds
                        double angleChanged = startAngle - currentAngle;
                        double value = _currVal;
                        value += angleChanged / (degreeMaxTurns / range);
                        if (value > _maxValue) {
                            value = _maxValue;
                        } else if (value < _minValue) {
                            value = _minValue;
                        }

                        setCurrVal(value);

                        rotateKnob((float) angleChanged);
                        startAngle = currentAngle;

                        if (_listener != null) {
                            _listener.onKnobValueChanged(_currVal);
                        }
                    }

                    break;

                case MotionEvent.ACTION_UP:
                    if (rotatorTouched) {
                        if (_listener != null) {
                            _listener.onKnobTouchEnd();
                        }
                    }
                    rotatorTouched = false;
                    break;
            }

            return true;
        }
    }

    private class OnKnobLongClickListener implements OnLongClickListener {

        @Override
        public boolean onLongClick(View view) {
            _listener.onKnobLongClick();
            return true;
        }
    }
}
