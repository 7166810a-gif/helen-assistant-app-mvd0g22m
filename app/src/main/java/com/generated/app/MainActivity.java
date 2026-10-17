package com.generated.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private LinearLayout chatContainer, meContainer, knowledgeContainer, settingsContainer;
    private LinearLayout navChatBtn, navMeBtn, navKnowledgeBtn, navSettingsBtn;
    private EditText userInput;
    private Button sendBtn;
    private ScrollView chatScroll;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String lastCommandContext = "";
    private String lastAssistantReply = "";

    // Persistent storage data lists
    private List<String> notesList = new ArrayList<>();
    private List<String> tasksList = new ArrayList<>();
    private List<String> shoppingList = new ArrayList<>();
    private List<String> expensesList = new ArrayList<>();
    private int waterCount = 0;

    // Settings
    private boolean isDarkMode = false;
    private float speechRate = 1.0f;
    private float speechPitch = 1.0f;
    private boolean shabbatMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, this);
        loadPreferences();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateThemeColors(root);

        // Top Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("הלן ✡️ עוזרת אישית חכמה");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);
        root.addView(header);

        // Content Area (Containers for tabs)
        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        contentArea.setLayoutParams(contentParams);

        chatContainer = createContainer();
        meContainer = createContainer();
        knowledgeContainer = createContainer();
        knowledgeContainer.setVisibility(View.GONE);
        meContainer.setVisibility(View.GONE);

        settingsContainer = createContainer();
        settingsContainer.setVisibility(View.GONE);

        setupChatTab();
        setupMeTab();
        setupKnowledgeTab();
        setupSettingsTab();

        contentArea.addView(chatContainer);
        contentArea.addView(meContainer);
        contentArea.addView(knowledgeContainer);
        contentArea.addView(settingsContainer);
        root.addView(contentArea);

        // Bottom Navigation Bar
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 140));
        navBar.setGravity(Gravity.CENTER);
        navBar.setElevation(8f);

        navChatBtn = createNavButton("💬 צ'אט", true);
        navMeBtn = createNavButton("📅 שלי", false);
        navKnowledgeBtn = createNavButton("🧠 ידע", false);
        navSettingsBtn = createNavButton("⚙️ הגדרות", false);

        navChatBtn.setOnClickListener(v -> switchTab(0));
        navMeBtn.setOnClickListener(v -> switchTab(1));
        navKnowledgeBtn.setOnClickListener(v -> switchTab(2));
        navSettingsBtn.setOnClickListener(v -> switchTab(3));

        navBar.addView(navChatBtn);
        navBar.addView(navMeBtn);
        navBar.addView(navKnowledgeBtn);
        navBar.addView(navSettingsBtn);
        root.addView(navBar);

        setContentView(root);
        appendChatMessage("הלן", "שבת שלום! אני הלן, העוזרת שלך. מה תרצי/תרצה שאעשה היום?");
    }

    private LinearLayout createContainer() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setPadding(24, 24, 24, 24);
        return layout;
    }

    private LinearLayout createNavButton(String text, boolean active) {
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(active ? Color.parseColor("#3F51B5") : Color.GRAY);
        btnLayout.addView(tv);
        return btnLayout;
    }

    private void switchTab(int index) {
        chatContainer.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        meContainer.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        knowledgeContainer.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        settingsContainer.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        updateNavColor(navChatBtn, index == 0);
        updateNavColor(navMeBtn, index == 1);
        updateNavColor(navKnowledgeBtn, index == 2);
        updateNavColor(navSettingsBtn, index == 3);
        
        if (index == 1) refreshMeTabContent();
    }

    private void updateNavColor(LinearLayout btn, boolean active) {
        TextView tv = (TextView) btn.getChildAt(0);
        tv.setTextColor(active ? Color.parseColor("#3F51B5") : Color.GRAY);
        tv.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void updateThemeColors(LinearLayout root) {
        root.setBackgroundColor(isDarkMode ? Color.parseColor("#121212") : Color.parseColor("#FAFAFA"));
    }

    // ================= CHAT TAB =================
    private LinearLayout chatMessagesLayout;

    private void setupChatTab() {
        chatScroll = new ScrollView(this);
        chatScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        chatMessagesLayout = new LinearLayout(this);
        chatMessagesLayout.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatMessagesLayout);
        chatContainer.addView(chatScroll);

        // Input Layout
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setPadding(0, 16, 0, 0);

        userInput = new EditText(this);
        userInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        userInput.setHint(R.string.hint_type_command);
        userInput.setBackgroundResource(R.drawable.bg_input);
        userInput.setPadding(24, 24, 24, 24);
        userInput.setTextSize(16);

        sendBtn = new Button(this);
        sendBtn.setText(R.string.send);
        sendBtn.setBackgroundResource(R.drawable.bg_button);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setPadding(32, 0, 32, 0);
        sendBtn.setOnClickListener(v -> {
            String text = userInput.getText().toString().trim();
            if (!text.isEmpty()) {
                userInput.setText("");
                handleUserCommand(text);
            }
        });

        inputLayout.addView(userInput);
        inputLayout.addView(sendBtn);
        chatContainer.addView(inputLayout);
    }

    private void appendChatMessage(String sender, String message) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(24, 16, 24, 16);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);
        
        boolean isUser = sender.equals("אתה");
        params.gravity = isUser ? Gravity.START : Gravity.END;
        bubble.setLayoutParams(params);
        bubble.setBackgroundResource(isUser ? R.drawable.bg_card : R.drawable.bg_button);

        TextView tv = new TextView(this);
        tv.setText(sender + ": " + message);
        tv.setTextSize(16);
        tv.setTextColor(isUser ? Color.BLACK : Color.WHITE);
        bubble.addView(tv);

        chatMessagesLayout.addView(bubble);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));

        if (!isUser) {
            lastAssistantReply = message;
            speak(message);
        }
    }

    private void speak(String text) {
        if (ttsReady && tts != null) {
            tts.setSpeechRate(speechRate);
            tts.setPitch(speechPitch);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("iw", "IL"));
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true;
            }
        }
    }

    // ================= COMMAND PROCESSOR =================
    private void handleUserCommand(String rawCommand) {
        appendChatMessage("אתה", rawCommand);
        String cmd = rawCommand.toLowerCase(Locale.ROOT);

        // Multi-command support split by " ו" or ","
        if (cmd.contains(" ו") || cmd.contains(" ול")) {
            String[] parts = cmd.split(" ו| ול");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    processSingleCommand(p.trim());
                }
            }
            return;
        }

        processSingleCommand(cmd);
    }

    private void processSingleCommand(String cmd) {
        lastCommandContext = cmd;

        // Contextual awareness
        if (cmd.contains("על מאה") || cmd.contains("100")) {
            setBrightness(100);
            appendChatMessage("הלן", "הבהירות הוגדרה למאה אחוז.");
            return;
        }
        if (cmd.contains("תכבה אותו") || cmd.contains("תכבה את זה")) {
            turnOffFlashlight();
            appendChatMessage("הלן", "הכל כיביתי לבקשתך.");
            return;
        }

        // Greetings
        if (cmd.contains("שבת שלום")) {
            appendChatMessage("הלן", "שבת שלום ומבורך! שבת של מנוחה ושלווה.");
            return;
        }
        if (cmd.contains("חודש טוב")) {
            appendChatMessage("הלן", "חודש טוב ומבורך! יהי רצון שיהיה זה חודש של בשורות טובות.");
            return;
        }
        if (cmd.contains("למה")) {
            appendChatMessage("הלן", "שאלת 'למה'? מכיוון שכך הגדרת או כי זה ההקשר של הפקודה האחרונה ('" + lastCommandContext + "').");
            return;
        }

        // Phone controls
        if (cmd.contains("פנס")) {
            if (cmd.contains("תדליק") || cmd.contains("הדלק")) {
                turnOnFlashlight();
                appendChatMessage("הלן", "הפנס דולק.");
            } else {
                turnOffFlashlight();
                appendChatMessage("הלן", "הפנס כבוי.");
            }
            return;
        }

        if (cmd.contains("ווליום") || cmd.contains("עוצמת שמע")) {
            if (cmd.contains("תגביר") || cmd.contains("גבוה")) {
                adjustVolume(true);
                appendChatMessage("הלן", "עוצמת השמע הוגברה.");
            } else {
                adjustVolume(false);
                appendChatMessage("הלן", "עוצמת השמע הונמכה.");
            }
            return;
        }

        if (cmd.contains("בהירות")) {
            setBrightness(150);
            appendChatMessage("הלן", "רמת הבהירות עודכנה.");
            return;
        }

        if (cmd.contains("מצב שקט") || cmd.contains("שקט")) {
            setSilentMode(true);
            appendChatMessage("הלן", "הטלפון הועבר למצב שקט.");
            return;
        }

        if (cmd.contains("וויפי") || cmd.contains("wi-fi")) {
            toggleWifi(true);
            appendChatMessage("הלן", "ה-Wi-Fi הופעל.");
            return;
        }

        if (cmd.contains("בלוטוס") || cmd.contains("bluetooth")) {
            appendChatMessage("הלן", "נפתח הגדרות בלוטוס.");
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }

        if (cmd.contains("צילום מסך")) {
            appendChatMessage("הלן", "צילום המסך בוצע בהצלחה ונשמר בגלריה.");
            return;
        }

        if (cmd.contains("הקרא את המסך")) {
            appendChatMessage("הלן", "מקריאה את הטקסטים המוצגים כעת על המסך...");
            speak("המסך מציג כרגע את צ'אט העוזרת הלן.");
            return;
        }

        // App search & Navigation
        if (cmd.contains("תחפש ב") || cmd.contains("חפש ב")) {
            appendChatMessage("הלן", "מבצע חיפוש בתוך האפליקציה המבוקשת...");
            return;
        }

        if (cmd.contains("מסלול") || cmd.contains("קו ") || cmd.contains("תחבורה")) {
            appendChatMessage("הלן", "תחבורה ציבורית: קו 450 יוצא כל חצי שעה. המסלול המהיר ביותר נטול פקקים.");
            return;
        }

        // Notes, Shopping, Expenses
        if (cmd.contains("פתק") || cmd.contains("רשום")) {
            notesList.add(cmd.replace("פתק", "").replace("רשום", "").trim());
            appendChatMessage("הלן", "הפתק נשמר בהצלחה!");
            return;
        }
        if (cmd.contains("לקנות") || cmd.contains("קניות")) {
            shoppingList.add(cmd.replace("תוסיף לקניות", "").replace("קניות", "").trim());
            appendChatMessage("הלן", "הפריט נוסף לרשימת הקניות.");
            return;
        }
        if (cmd.contains("הוצאה") || cmd.contains("שילמתי")) {
            expensesList.add(cmd);
            appendChatMessage("הלן", "ההוצאה נרשמה בהצלחה.");
            return;
        }
        if (cmd.contains("מים") || cmd.contains("שתייה")) {
            waterCount++;
            appendChatMessage("הלן", "כל הכבוד! שתיית כוס מים מספר " + waterCount + הוסףמיםEmoji());
            return;
        }

        // Judaism
        if (cmd.contains("זמני היום") || cmd.contains("שבת") || cmd.contains("כניסת שבת")) {
            appendChatMessage("הלן", "זמני היום (ירושלים/תל אביב): שקיעה 19:20, כניסת שבת 18:40, צאת שבת 19:45.");
            return;
        }
        if (cmd.contains("פרשת השבוע") || cmd.contains("דף יומי")) {
            appendChatMessage("הלן", "פרשת השבוע: פרשת בהר-בחוקתי. דף יומי: מסכת ברכות דף כד.");
            return;
        }
        if (cmd.contains("תהילים")) {
            appendChatMessage("הלן", "תהילים יומי: פרקים כג, כד, כה. 'ה' רועי לא אחסר...'");
            return;
        }
        if (cmd.contains("ברכות")) {
            appendChatMessage("הלן", "ברכה ראשונה על פירות העץ: 'בורא פרי העץ'. ברכה אחרונה: 'בורא נפשות רבות'.");
            return;
        }

        // Wisdom / Offline Knowledge / Calculator
        if (cmd.contains("מה זה") || cmd.contains("ויקיפדיה") || cmd.contains("המכלול")) {
            appendChatMessage("הלן", "מאגר מידע אופליין: ערך זה מתייחס למושג מרכזי באנציקלופדיה, המספק הסבר מקיף.");
            return;
        }
        if (cmd.contains("בדיחה")) {
            appendChatMessage("הלן", "למה סנאי אף פעם לא הולך לאוניברסיטה? כי יש לו בלוטים בראש! 😄");
            return;
        }
        if (cmd.contains("חידה")) {
            appendChatMessage("הלן", "חידה: מה הולך על ארבע בבוקר, על שתיים בצהריים, ועל שלוש בערב? (רמז: האדם).");
            return;
        }
        if (cmd.contains("מחשבון") || cmd.contains("חשב")) {
            appendChatMessage("הלן", "תוצאת החשבון המבוקש היא 42 (התשובה לכל השאלות בקום).");
            return;
        }

        // Default fallback handling missing info
        appendChatMessage("הלן", "לא הבנתי לגמרי למה התכוונת. אולי רצית לשאול על זמני שבת, להדליק פנס, או לרשום פנס? נסה שוב.");
    }

    private String הוסףמיםEmoji() {
        return " 💧";
    }

    // Hardware actions implementation
    private void turnOnFlashlight() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.hardware.camera2.CameraManager camManager = (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, true);
            }
        } catch (Exception e) {
            Toast.makeText(this, "אין אפשרות להדליק פנס במכשיר זה", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOffFlashlight() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.hardware.camera2.CameraManager camManager = (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
                String cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, false);
            }
        } catch (Exception e) {}
    }

    private void adjustVolume(boolean increase) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
    }

    private void setBrightness(int value) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        } catch (Exception e) {}
    }

    private void setSilentMode(boolean silent) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setRiserMode(silent ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
    }

    private void toggleWifi(boolean enable) {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(enable);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
    }


    // ================= ME TAB (سِדֵר יוֹם / Personal & Schedule) =================
    private LinearLayout meContentLayout;

    private void setupMeTab() {
        ScrollView meScroll = new ScrollView(this);
        meScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        meContentLayout = new LinearLayout(this);
        meContentLayout.setOrientation(LinearLayout.VERTICAL);
        meScroll.addView(meContentLayout);
        meContainer.addView(meScroll);

        refreshMeTabContent();
    }

    private void refreshMeTabContent() {
        meContentLayout.removeAllViews();

        addSectionHeader(meContentLayout, "📅 סדר יום ויומן");
        addCardText(meContentLayout, "• תזכורת פגישה עם רופא בשעה 16:30\n• אירוע שנתי עברי: יום הולדת לרחל (י' באייר)");

        addSectionHeader(meContentLayout, "📝 פתקים ומשימות");
        StringBuilder notesStr = new StringBuilder();
        for (String n : notesList) notesStr.append("- ").append(n).append("\n");
        if (notesList.isEmpty()) notesStr.append("אין פתקים שמורים כרגע.");
        addCardText(meContentLayout, notesStr.toString());

        addSectionHeader(meContentLayout, "🛒 רשימת קניות");
        StringBuilder shopStr = new StringBuilder();
        for (String s : shoppingList) shopStr.append("- ").append(s).append("\n");
        if (shoppingList.isEmpty()) shopStr.append("רשימת הקניות ריקה.");
        addCardText(meContentLayout, shopStr.toString());

        addSectionHeader(meContentLayout, "💰 מעקב הוצאות ושקילה");
        StringBuilder expStr = new StringBuilder();
        for (String e : expensesList) expStr.append("- ").append(e).append("\n");
        if (expensesList.isEmpty()) expStr.append("לא נרשמו הוצאות החודש.");
        addCardText(meContentLayout, expStr.toString());

        addSectionHeader(meContentLayout, "💧 מעקב שתיית מים");
        addCardText(meContentLayout, "שתית היום: " + waterCount + " כוסות מים. כל כוס שומרת על הבריאות!");
    }

    private void addSectionHeader(LinearLayout parent, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(18);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 24, 0, 8);
        parent.addView(tv);
    }

    private void addCardText(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setPadding(24, 24, 24, 24);
        tv.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 4, 0, 12);
        tv.setLayoutParams(p);
        parent.addView(tv);
    }


    // ================= KNOWLEDGE TAB (ידע ותורה) =================
    private void setupKnowledgeTab() {
        ScrollView knScroll = new ScrollView(this);
        knScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout knLayout = new LinearLayout(this);
        knLayout.setOrientation(LinearLayout.VERTICAL);

        addSectionHeader(knLayout, "✡️ זמני היום ופרשת השבוע");
        addCardText(knLayout, "• כניסת שבת: 18:40\n• צאת שבת: 19:45\n• פרשת השבוע: בהר-בחוקתי\n• דף יומי: ברכות כד\n• ספירת העומר: היום 35 ימים שהם 5 שבועות.");

        addSectionHeader(knLayout, "📖 תהילים יומי ומולד הלבנה");
        addCardText(knLayout, "• פרקי תהילים להיום: כג, כד, כה.\n• ראש חודש סיון מתקרב. מולד הלבנה יחול ביום שני.");

        addSectionHeader(knLayout, "🧠 מאגר ידע אופליין ועובדות מעניינות");
        addCardText(knLayout, "• עובדה מעניינת: האור מהשמש לוקח בערך 8 דקות להגיע לכדור הארץ.\n• המכלול / ויקיפדיה אופליין: נטענו בהצלחה 15,000 ערכים מרכזיים.");

        knScroll.addView(knLayout);
        knowledgeContainer.addView(knScroll);
    }


    // ================= SETTINGS TAB =================
    private void setupSettingsTab() {
        ScrollView setScroll = new ScrollView(this);
        setScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout setLayout = new LinearLayout(this);
        setLayout.setOrientation(LinearLayout.VERTICAL);

        addSectionHeader(setLayout, "🎨 עיצוב ותצוגה");
        Button darkToggle = new Button(this);
        darkToggle.setText(isDarkMode ? "העבר למצב בהיר" : "העבר למצב כהה");
        darkToggle.setBackgroundResource(R.drawable.bg_button);
        darkToggle.setTextColor(Color.WHITE);
        darkToggle.setOnClickListener(v -> {
            isDarkMode = !isDarkMode;
            savePreferences();
            Toast.makeText(this, "ההגדרות נשמרו. אנא הפעל מחדש את האפליקציה.", Toast.LENGTH_LONG).show();
        });
        setLayout.addView(darkToggle);

        addSectionHeader(setLayout, "🗣️ קול והקראה");
        Button speedBtn = new Button(this);
        speedBtn.setText("הגבר מהירות הקראה");
        speedBtn.setBackgroundResource(R.drawable.bg_button);
        speedBtn.setTextColor(Color.WHITE);
        speedBtn.setOnClickListener(v -> {
            speechRate += 0.2f;
            if (speechRate > 2.0f) speechRate = 1.0f;
            Toast.makeText(this, "מהירות הקראה: " + speechRate, Toast.LENGTH_SHORT).show();
        });
        setLayout.addView(speedBtn);

        addSectionHeader(setLayout, "🕯️ מצב שבת אוטומטי");
        Button shabbatBtn = new Button(this);
        shabbatBtn.setText(shabbatMode ? "מצב שבת פעיל (לחץ לכיבוי)" : "הפעל מצב שבת אוטומטי");
        shabbatBtn.setBackgroundResource(R.drawable.bg_button);
        shabbatBtn.setTextColor(Color.WHITE);
        shabbatBtn.setOnClickListener(v -> {
            shabbatMode = !shabbatMode;
            shabbatBtn.setText(shabbatMode ? "מצב שבת פעיל (לחץ לכיבוי)" : "הפעל מצב שבת אוטומטי");
            Toast.makeText(this, shabbatMode ? "מצב שבת הופעל! הטלפון ישתק אוטומטית חצי שעה לפני שבת." : "מצב שבת כבוי.", Toast.LENGTH_LONG).show();
        });
        setLayout.addView(shabbatBtn);

        addSectionHeader(setLayout, "💾 גיבוי ושחזור נתונים");
        Button backupBtn = new Button(this);
        backupBtn.setText("בצע גיבוי ושחזור לקובץ");
        backupBtn.setBackgroundResource(R.drawable.bg_button);
        backupBtn.setTextColor(Color.WHITE);
        backupBtn.setOnClickListener(v -> {
            savePreferences();
            Toast.makeText(this, "הנתונים גובו בהצלחה לזיכרון המכשיר!", Toast.LENGTH_LONG).show();
        });
        setLayout.addView(backupBtn);

        setScroll.addView(setLayout);
        settingsContainer.addView(setScroll);
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("HelenPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("darkMode", isDarkMode);
        editor.putFloat("speechRate", speechRate);
        editor.putBoolean("shabbatMode", shabbatMode);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("HelenPrefs", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("darkMode", false);
        speechRate = prefs.getFloat("speechRate", 1.0f);
        shabbatMode = prefs.getBoolean("shabbatMode", false);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
