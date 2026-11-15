package local.casa.tv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends Activity {
    private static final int INK = 0xfff5f2ec;
    private static final int MUTED = 0xffb8b5b0;
    private static final String TV_INPUT_APP = "com.mediatek.wwtv.tvcenter";
    private static final String SETTINGS_APP = "com.android.tv.settings";
    private static final Map<String, Brand> BRANDS = new LinkedHashMap<>();

    static {
        BRANDS.put("com.google.android.youtube.tv", new Brand("youtube", 0xffed493d, 58));
        BRANDS.put("com.netflix.ninja", new Brand("netflix", 0xffec3442, 28));
        BRANDS.put("com.amazon.amazonvideo.livingroom", new Brand("primevideo", 0xff45b7e9, 76));
        BRANDS.put("com.disney.disneyplus", new Brand("disneyplus", 0xff779cff, 94));
        BRANDS.put("com.wbd.stream", new Brand("max", 0xffa773ff, 78));
        BRANDS.put("tv.twitch.android.app", new Brand("twitch", 0xffad75f1, 40));
        BRANDS.put("com.spotify.tv.android", new Brand("spotify", 0xff1ed760, 43));
    }

    private String appSignature;
    private HomePreferences preferences;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = HomePreferences.read(this);
    }

    @Override public void onResume() {
        super.onResume();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(query, 0);
        Collator collator = Collator.getInstance();
        apps.sort((left, right) -> collator.compare(left.loadLabel(getPackageManager()).toString(),
                right.loadLabel(getPackageManager()).toString()));
        Map<String, ResolveInfo> installed = new LinkedHashMap<>();
        for (ResolveInfo app : apps) {
            String id = app.activityInfo.packageName;
            if (!id.equals(getPackageName()) && !id.equals(SETTINGS_APP)) installed.putIfAbsent(id, app);
        }

        StringBuilder signature = new StringBuilder();
        for (ResolveInfo app : installed.values()) {
            signature.append(app.activityInfo.packageName).append('/')
                    .append(app.activityInfo.name).append(';');
        }
        if (signature.toString().equals(appSignature)) return;
        appSignature = signature.toString();

        ResolveInfo television = installed.remove(TV_INPUT_APP);
        Map<String, ResolveInfo> ordered = new LinkedHashMap<>();
        for (String id : BRANDS.keySet()) {
            ResolveInfo app = installed.remove(id);
            if (app != null) ordered.put(id, app);
        }
        ordered.putAll(installed);
        buildHome(ordered, television);
    }

    private void buildHome(Map<String, ResolveInfo> apps, ResolveInfo television) {
        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setVerticalScrollBarEnabled(false);
        screen.setBackgroundColor(Color.BLACK);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(32), 0, dp(20));
        screen.addView(body);

        int windowWidth = getResources().getDisplayMetrics().widthPixels;
        int gutter = Math.min(dp(52), windowWidth / 14);
        body.addView(clockHeader(gutter), new LinearLayout.LayoutParams(-1, dp(156)));
        addSection(body, "Apps", gutter);

        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        shelf.setOverScrollMode(HorizontalScrollView.OVER_SCROLL_NEVER);
        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        row.setPadding(dp(4), dp(6), dp(4), dp(6));
        shelf.addView(row);
        LinearLayout.LayoutParams shelfSlot = new LinearLayout.LayoutParams(-1, -2);
        shelfSlot.setMarginStart(gutter - dp(4));
        shelfSlot.setMarginEnd(gutter - dp(4));
        body.addView(shelf, shelfSlot);

        int available = windowWidth - 2 * gutter;
        int visibleCards = Math.max(2, Math.min(6, available / dp(125)));
        int tileWidth = (available - (visibleCards - 1) * dp(10)) / visibleCards;
        LinearLayout first = null;
        for (ResolveInfo app : apps.values()) {
            LinearLayout tile = appTile(app);
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(tileWidth, dp(110));
            if (first != null) slot.setMarginStart(dp(10));
            row.addView(tile, slot);
            if (first == null) first = tile;
        }
        shelf.setVisibility(apps.isEmpty() ? HorizontalScrollView.GONE : HorizontalScrollView.VISIBLE);

        if (television != null) {
            LinearLayout inputs = new LinearLayout(this);
            inputs.setOrientation(LinearLayout.VERTICAL);
            inputs.setPadding(gutter, dp(54), gutter, 0);
            inputs.addView(label("Entradas", 14, MUTED));
            TextView live = pill("TV ao vivo", launchIntent(television));
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(dp(144), dp(42));
            slot.topMargin = dp(12);
            inputs.addView(live, slot);
            body.addView(inputs);
        }

        if (!preferences.signature.isEmpty()) {
            addSignature(body, gutter);
        }

        setContentView(screen);
        if (first != null) first.requestFocus();
    }

    private void addSignature(LinearLayout body, int gutter) {
        TextView signature = label("", 14, MUTED);
        signature.setSingleLine(true);
        signature.setEllipsize(TextUtils.TruncateAt.END);
        signature.setMaxWidth(dp(260));
        SpannableString names = new SpannableString(preferences.signature + "  ♥");
        names.setSpan(new ForegroundColorSpan(0xfff08a9f),
                names.length() - 1, names.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signature.setText(names);
        LinearLayout.LayoutParams signatureSlot = new LinearLayout.LayoutParams(-2, -2);
        signatureSlot.gravity = Gravity.END;
        signatureSlot.setMarginEnd(gutter);
        signatureSlot.topMargin = dp(22);
        body.addView(signature, signatureSlot);
    }

    private FrameLayout clockHeader(int gutter) {
        FrameLayout header = new FrameLayout(this);
        int center = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        PixelClock clock = new PixelClock(this);
        FrameLayout.LayoutParams clockSlot = new FrameLayout.LayoutParams(-1, dp(84), center);
        clockSlot.topMargin = dp(25);
        header.addView(clock, clockSlot);

        TextClock date = new TextClock(this);
        String format = "EEEE, d 'de' MMMM";
        date.setFormat12Hour(format);
        date.setFormat24Hour(format);
        date.setGravity(Gravity.CENTER);
        date.setTextColor(MUTED);
        date.setTextSize(14);
        FrameLayout.LayoutParams dateSlot = new FrameLayout.LayoutParams(-1, dp(24), center);
        dateSlot.topMargin = dp(128);
        header.addView(date, dateSlot);

        TextView settings = pill("Configurações", new Intent(Settings.ACTION_SETTINGS));
        FrameLayout.LayoutParams settingsSlot = new FrameLayout.LayoutParams(dp(122), dp(38),
                Gravity.TOP | Gravity.END);
        settingsSlot.setMarginEnd(gutter);
        settingsSlot.topMargin = dp(11);
        header.addView(settings, settingsSlot);

        if (preferences.hasWeather()) {
            WeatherWidget weather = new WeatherWidget(this, preferences);
            FrameLayout.LayoutParams weatherSlot = new FrameLayout.LayoutParams(dp(164), -2,
                    Gravity.TOP | Gravity.START);
            weatherSlot.setMarginStart(gutter);
            weatherSlot.topMargin = dp(11);
            header.addView(weather, weatherSlot);
        }
        return header;
    }

    private void addSection(LinearLayout layout, String title, int gutter) {
        TextView heading = label(title, 14, MUTED);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(gutter, dp(5), gutter, dp(7));
        layout.addView(heading);
    }

    private LinearLayout appTile(ResolveInfo app) {
        String name = app.loadLabel(getPackageManager()).toString();
        Brand brand = BRANDS.get(app.activityInfo.packageName);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(8), 0, dp(8), 0);
        tile.setGravity(Gravity.CENTER);
        tile.setContentDescription(name);

        FrameLayout artwork = new FrameLayout(this);
        tile.addView(artwork, new LinearLayout.LayoutParams(-1, 0, 1));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (brand != null) {
            try (InputStream mark = getAssets().open("marks/" + brand.asset + ".png")) {
                icon.setImageBitmap(BitmapFactory.decodeStream(mark));
            } catch (IOException error) {
                icon.setImageDrawable(app.loadIcon(getPackageManager()));
            }
        } else {
            icon.setImageDrawable(app.loadIcon(getPackageManager()));
            GradientDrawable circle = surface(dp(24), 0xff252528, Color.TRANSPARENT);
            icon.setBackground(circle);
            icon.setClipToOutline(true);
        }
        int width = dp(brand == null ? 43 : brand.width);
        int height = brand == null ? width : dp(48);
        artwork.addView(icon, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));

        TextView caption = label(name, 12, MUTED);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(caption, new LinearLayout.LayoutParams(-1, dp(29)));
        tile.setFocusable(true);
        tile.setOnClickListener(view -> open(launchIntent(app), name));
        int accent = brand == null ? 0xffd4cdc0 : brand.color;
        tile.setBackground(tileSurface(false, accent));
        tile.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(tileSurface(focused, accent));
            caption.setTextColor(focused ? INK : MUTED);
        });
        return tile;
    }

    private LayerDrawable tileSurface(boolean focused, int accent) {
        GradientDrawable edge = surface(dp(16), Color.TRANSPARENT,
                focused ? tint(accent, 67) : Color.TRANSPARENT);
        GradientDrawable card = surface(dp(14), focused ? tint(accent, 29) : 0xff0a0a0a,
                focused ? accent : 0xff292929);
        LayerDrawable layers = new LayerDrawable(new GradientDrawable[]{edge, card});
        int inset = dp(2);
        layers.setLayerInset(1, inset, inset, inset, inset);
        return layers;
    }

    private TextView pill(String name, Intent action) {
        TextView button = label(name, 13, INK);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setBackground(surface(dp(22), 0xff0c0c0c, 0xff303030));
        button.setOnFocusChangeListener((view, focused) -> view.setBackground(surface(dp(22),
                focused ? 0xff292826 : 0xff0c0c0c, focused ? INK : 0xff303030)));
        button.setOnClickListener(view -> open(action, name));
        return button;
    }

    private GradientDrawable surface(int radius, int fill, int rim) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(dp(1), rim);
        return shape;
    }

    private static int tint(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    private static Intent launchIntent(ResolveInfo app) {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(new ComponentName(app.activityInfo.packageName, app.activityInfo.name));
    }

    private void open(Intent action, String name) {
        try {
            startActivity(action);
        } catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, "Não foi possível abrir " + name, Toast.LENGTH_SHORT).show();
        }
    }

    private TextView label(String value, int size, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Brand {
        final String asset;
        final int color;
        final int width;

        Brand(String asset, int color, int width) {
            this.asset = asset;
            this.color = color;
            this.width = width;
        }
    }
}
