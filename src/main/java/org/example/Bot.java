package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//http://cbu.uz/uzc/arkhiv-kursov-valyut/xml/

public class Bot extends TelegramLongPollingBot {

    private Logger logger = new Logger();
    private final int ERR_CODE = -1;
    private String path_app = "application.properties";
    
    private Long getAdminId() {
        try {
            FileReader f = new FileReader(path_app);
            Properties properties = new Properties();
            properties.load(f);
            Long adminId = Long.parseLong(properties.getProperty("admin.id"));
            return adminId;
        } catch (IOException e) {
            logger.loge("File topilmadi");
        }

        return Long.valueOf(-1);
    }

    private Long ADMIN_ID = Long.valueOf(0);

    public Bot(){
        ADMIN_ID = getAdminId();
    }

    private static String getStatus() throws Exception {
        String get_text = "";
        String n[] = {"Kasallanganlar\uD83E\uDD12    ", "Tuzalganlar\uD83E\uDD24     ",
                "Vafot etganlar⚰️    "};
        int i = 0;
        URL url = new URL("https://www.gazeta.uz/oz/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        String html = "";
        String line = "";
        BufferedReader buffer = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((line = buffer.readLine()) != null) {
            html += line + "\n";
        }
        String reg = "<div class=\"block-row-item row-value\"><span>";
        Pattern ptr = Pattern.compile(reg);
        Matcher mt = ptr.matcher(html);
        while (mt.find()) {
            String xml = html.substring(mt.start() + 44, mt.end() + 10);
            String as = xml.substring(0, xml.indexOf('<'));
            get_text += n[i] + as + "\n";
            i++;
        }
        buffer.close();
        con.disconnect();

        return get_text;
    }

    private List<Long> foydalanuvchilar = new ArrayList<>();

    private boolean in_array(Long id, List<Long> f) {
        for (int i = 0; i < f.size(); i++) {
            if (id.equals(f.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void onUpdateReceived(Update update) {

        List<List<InlineKeyboardButton>> main_board = new ArrayList<List<InlineKeyboardButton>>();
        List<List<InlineKeyboardButton>> min_board = new ArrayList<List<InlineKeyboardButton>>();

        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            SendMessage msg = new SendMessage();
            msg.setParseMode(ParseMode.HTML);
            Chat chat = update.getMessage().getChat();
            RestrictChatMember mute = new RestrictChatMember();
            ZonedDateTime zd = ZonedDateTime.now();
            boolean is_creator = update.getMessage().getFrom().getId().equals(ADMIN_ID);
            boolean is_admin = false;
            Long user_id = update.getMessage().getFrom().getId();

            if (update.getMessage().isSuperGroupMessage() || update.getMessage().isGroupMessage()) {

                GetChatAdministrators administrators = new GetChatAdministrators();
                administrators.setChatId(chat_id);
                List<ChatMember> admins = null;
                try {
                    admins = execute(administrators);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                for (ChatMember c : admins) {
                    if (c.getUser().getId().equals(user_id)) {
                        is_admin = true;
                        break;
                    }
                    is_admin = false;
                }

                if (message_text.equals("/members_count") && (is_admin || is_creator)) {
                    try {
                        GetChatMemberCount count = new GetChatMemberCount();
                        count.setChatId(chat_id);
                        msg.setChatId(chat_id);
                        msg.setText("A'zolar soni: " + execute(count));
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.equals("/gid") && (is_admin || is_creator)) {
                    msg.setChatId(chat_id);
                    msg.setText("Guruh id: <code>" + chat.getId() + "</code>\n" +
                            "Guruh niki: <code>" + chat.getTitle() + "</code>\n" +
                            "Guruh linki: @" + chat.getUserName());

                }

                if(message_text.equals("/ban") && update.getMessage().isReply()
                && is_creator ){
                    BanChatMember ban = new BanChatMember(String.valueOf(chat_id), update.getMessage().
                            getReplyToMessage().getFrom().getId());
                    try{
                        execute(ban);
                    }catch (TelegramApiException e){
                        logger.loge(String.format("Can't ban member with id %d", update.getMessage()
                                .getReplyToMessage().getFrom().getId()));
                    }
                }

                if (message_text.equals("/kick") && update.getMessage().isReply()
                        && (is_admin || is_creator)) {
                    BanChatMember ki = new BanChatMember(String.valueOf(chat_id), update.getMessage()
                            .getReplyToMessage().getFrom().getId());
                    UnbanChatMember un = new UnbanChatMember(String.valueOf(chat_id),
                            update.getMessage().getReplyToMessage().getFrom().getId());
                    try {
                        execute(ki);
                        execute(un);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.equals("dir")) {
                    try {
                        File f = new File("mkfull.txt");
                        f.createNewFile();
                        SendMessage mmm = new SendMessage();
                        mmm.setText("Lezzy");
                        mmm.setChatId(chat_id);
                        execute(mmm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.equals("/leave_chat") &&
                        update.getMessage().getFrom().getId().equals(ADMIN_ID)) {
                    LeaveChat leave = new LeaveChat();
                    leave.setChatId(chat_id);

                    try {
                        execute(leave);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.startsWith("/brt=") && (is_admin || is_creator)) {
                    String mv = message_text.substring(5);
                    SetChatTitle title = new SetChatTitle(String.valueOf(chat_id), mv);

                    try {
                        execute(title);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.equals("/pin") && update.getMessage().isReply()
                        && (is_admin || is_creator)) {
                    PinChatMessage pin = new PinChatMessage();
                    pin.setChatId(chat_id);
                    pin.setMessageId(update.getMessage().getReplyToMessage().getMessageId());

                    DeleteMessage del = new DeleteMessage(String.valueOf(chat_id),
                            update.getMessage().getMessageId());

                    try {
                        execute(pin);
                        execute(del);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

                if (message_text.equals("/unpin") && (is_admin || is_creator)) {
                    UnpinChatMessage unpin = new UnpinChatMessage(String.valueOf(chat_id));

                    DeleteMessage del = new DeleteMessage(String.valueOf(chat_id),
                            update.getMessage().getMessageId());

                    try {
                        execute(unpin);
                        execute(del);
                    } catch (TelegramApiException e) {
                        logger.loge(String.format("Can't delete message{%d}\nCan't unpin in the chat{%d}",
                                update.getMessage().getMessageId(), chat_id));
                    }
                }

                if (update.getMessage().hasEntities() && !is_admin) {
                    List<MessageEntity> en = new ArrayList<>();
                    en = update.getMessage().getEntities();
                    msg.setChatId(chat_id);
                    String type = en.get(0).getType();
                    DeleteMessage del = new DeleteMessage();
                    if (type.equals("url") || type.equals("text_link")) {
                        del.setChatId(chat_id);
                        del.setMessageId(update.getMessage().getMessageId());

                        try {
                            execute(del);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (message_text.equals("/delete") && update.getMessage().isReply() && (is_admin || is_creator)) {
                    DeleteMessage del = new DeleteMessage(String.valueOf(chat_id), update.getMessage()
                            .getReplyToMessage().getMessageId());
                    DeleteMessage del1 = new DeleteMessage(String.valueOf(chat_id),
                            update.getMessage().getMessageId());

                    try {
                        execute(del);
                        execute(del1);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (message_text.startsWith("calc")) {
                SendMessage msgt = new SendMessage();
                msgt.setParseMode(ParseMode.HTML);
                msgt.setChatId(chat_id);
                msgt.setText("Hisoblanmoqda...");
                String would_be_calc = message_text.substring(5);
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("nashorn");
                try {
                    execute(msgt);
                    int aimp = (int) engine.eval(would_be_calc);
                    String kemp = String.format("<code>%d</code>", aimp);
                    msgt.setText(kemp);
                } catch (Exception e) {
                    e.printStackTrace();
                    msgt.setText("<b>Xatolik!</b>");
                }

                try {
                    execute(msgt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (message_text.equals("/subscriber") && update.getMessage().getFrom().getId().equals(
                    ADMIN_ID)) {
                SendMessage h = new SendMessage();
                h.setChatId((long) ADMIN_ID);
                h.setParseMode(ParseMode.MARKDOWN);
                String no_list = "Foydalanuvchilar soni: *" + foydalanuvchilar.size() + "* ta\n";
                for (int i = 0; i < foydalanuvchilar.size(); i++) {
                    no_list += "[" + (i + 1) + "-foydalanuvchi](tg://user?id="
                            + foydalanuvchilar.get(i) + ") 🆔 " + foydalanuvchilar.get(i) + "\n";
                }
                h.setText(no_list);

                try {
                    execute(h);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (message_text.startsWith("/send_all=") && update.getMessage().getFrom()
                    .getId().equals(
                            ADMIN_ID
                    )) {
                String sub = message_text.substring(10);
                for (int i = 0; i < foydalanuvchilar.size(); i++) {
                    SendMessage h = new SendMessage();
                    h.setText(sub);
                    h.setChatId((long) foydalanuvchilar.get(i));

                    try {
                        execute(h);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (message_text.trim().equals("/me") && update.getMessage().isUserMessage()) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> list = new ArrayList<List<InlineKeyboardButton>>();
                List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton mana = new InlineKeyboardButton();
                mana.setText("Mana");
                mana.setUrl("https://t.me/" + update.getMessage().getFrom().getUserName());
                row.add(mana);
                list.add(row);
                markup.setKeyboard(list);

                msg.setChatId(chat_id);
                msg.setReplyMarkup(markup);
                msg.setText("Link tayyor");

            }

            if (message_text.startsWith("/id=") && update.getMessage().isUserMessage()) {
                String text = message_text.substring(4);
                SendMessage id_finder = new SendMessage();
                id_finder.setParseMode(ParseMode.MARKDOWN);
                id_finder.setChatId(chat_id);
                Integer id = Integer.parseInt(text);
                id_finder.setText("Siz qidirgan odam\uD83D\uDD0E: [" + text + "](tg://user?id=" + id + ")");

                try {
                    execute(id_finder);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (update.getMessage().isUserMessage()) {
                if (!in_array(update.getMessage().getFrom().getId(), foydalanuvchilar)) {
                    User foydalanuvchining_uzi = update.getMessage().getFrom();
                    Long foydalanuvchi_id = foydalanuvchining_uzi.getId();

                    LocalDateTime l = LocalDateTime.now();
                    l = l.plusHours(5);
                    String time = String.format("%02d:%02d:%02d",
                            l.getHour(), l.getMinute(), l.getSecond());
                    foydalanuvchilar.add(foydalanuvchi_id);
                    SendMessage adminga_log = new SendMessage();
                    adminga_log.setParseMode(ParseMode.HTML);
                    adminga_log.setChatId((long) ADMIN_ID);
                    adminga_log.setText("Yangi a`zo qo'shildi.\n" +
                            foydalanuvchining_uzi.getFirstName() + "\n" +
                            foydalanuvchining_uzi.getId() + "\n" +
                            foydalanuvchining_uzi.getUserName() + "\n" +
                            "<b>Qo'shilgan vaqti: " + time + "</b>");

                    try {
                        execute(adminga_log);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (message_text.trim().equals("/start") && update.getMessage().isUserMessage()) {
                InlineKeyboardMarkup main_markup = new InlineKeyboardMarkup();

                msg.setChatId(chat_id);
                msg.setText("Salom <b>" + update.getMessage().getFrom().getFirstName() + "</b>");
                List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
                List<InlineKeyboardButton> row1 = new ArrayList<InlineKeyboardButton>();
                List<InlineKeyboardButton> row2 = new ArrayList<InlineKeyboardButton>();
                List<InlineKeyboardButton> row3 = new ArrayList<InlineKeyboardButton>();
                List<InlineKeyboardButton> row4 = new ArrayList<>();

                InlineKeyboardButton back = new InlineKeyboardButton("<----Back");
                back.setCallbackData("dashboard");
                row4.add(back);

                InlineKeyboardButton weather = new InlineKeyboardButton("Ob-havo ⛰");
                weather.setCallbackData("obhavo");
                row3.add(weather);
                InlineKeyboardButton rate = new InlineKeyboardButton("Valyuta\uD83D\uDCB5");
                rate.setCallbackData("valyuta");
                row3.add(rate);
                InlineKeyboardButton datas = new InlineKeyboardButton("Ma`lumotlar \uD83D\uDCBD");
                datas.setCallbackData("info");
                row2.add(datas);
                InlineKeyboardButton commands = new InlineKeyboardButton("Kommandalar \uD83D\uDCF2");
                commands.setCallbackData("commands");
                row2.add(commands);
                InlineKeyboardButton covid = new InlineKeyboardButton("Koronavirus \uD83E\uDDA0");
                covid.setCallbackData("clicked_natija");
                row1.add(covid);
                InlineKeyboardButton mooncat = new InlineKeyboardButton("Mooncat \uD83C\uDF15");
                mooncat.setCallbackData("mooncat");
                row1.add(mooncat);
                InlineKeyboardButton admin = new InlineKeyboardButton("Admin \uD83D\uDC68\u200D\uD83D\uDCBB");
                admin.setUrl("https://t.me/Zukunf");
                row.add(admin);
                InlineKeyboardButton add_group = new InlineKeyboardButton("Guruhga qo'shish➕");
                add_group.setUrl("https://t.me/clonemaincampbot?startgroup=test");
                row.add(add_group);

                main_board.add(row);
                main_board.add(row1);
                main_board.add(row2);
                main_board.add(row3);
                main_board.add(row4);

                main_markup.setKeyboard(main_board);
                msg.setReplyMarkup(main_markup);
            }

            try {
                if (!msg.getChatId().equals(null))
                    execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        if (update.hasCallbackQuery()) {
            String mat1 = "\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15";
            String mat2 = "\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14";
            String mat3 = "\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13";
            String mat4 = "\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12";
            String mat5 = "\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11";
            String mat6 = "\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17\uD83C\uDF18";
            String mat7 = "\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16\uD83C\uDF17";
            String mat8 = "\uD83C\uDF17\uD83C\uDF18\uD83C\uDF11\uD83C\uDF12\uD83C\uDF13\uD83C\uDF14\uD83C\uDF15\uD83C\uDF16";
            final String matrix[] = {mat1, mat2, mat3, mat4, mat5, mat6, mat7, mat8};
            String call_data = update.getCallbackQuery().getData();
            final long message_id = update.getCallbackQuery().getMessage().getMessageId();
            final long chat_id = update.getCallbackQuery().getMessage().getChatId();
            User callback_user = update.getCallbackQuery().getFrom();

            if (call_data.equals("obhavo")) {

                InlineKeyboardMarkup region_markup = new InlineKeyboardMarkup();
                List<InlineKeyboardButton> row1 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton tashkent = new InlineKeyboardButton("Toshkent");
                tashkent.setCallbackData("tashkent");
                row1.add(tashkent);
                InlineKeyboardButton andijan = new InlineKeyboardButton("Andijon");
                andijan.setCallbackData("andijan");
                row1.add(andijan);

                List<InlineKeyboardButton> row2 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton bukhara = new InlineKeyboardButton("Buxoro");
                bukhara.setCallbackData("bukhara");
                row2.add(bukhara);

                InlineKeyboardButton jizzakh = new InlineKeyboardButton("Jizzax");
                jizzakh.setCallbackData("jizzakh");
                row2.add(jizzakh);

                List<InlineKeyboardButton> row3 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton gulistan = new InlineKeyboardButton("Guliston");
                gulistan.setCallbackData("gulistan");
                row3.add(gulistan);
                InlineKeyboardButton zarafshan = new InlineKeyboardButton("Zarafshon");
                zarafshan.setCallbackData("zarafshan");
                row3.add(zarafshan);

                List<InlineKeyboardButton> row4 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton karshi = new InlineKeyboardButton("Qarshi");
                karshi.setCallbackData("karshi");
                row4.add(karshi);
                InlineKeyboardButton navoi = new InlineKeyboardButton("Navoiy");
                navoi.setCallbackData("navoi");
                row4.add(navoi);

                List<InlineKeyboardButton> row5 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton namangan = new InlineKeyboardButton("Namangan");
                namangan.setCallbackData("namangan");
                row5.add(namangan);
                InlineKeyboardButton nukus = new InlineKeyboardButton("Nukus");
                nukus.setCallbackData("nukus");
                row5.add(nukus);
                List<InlineKeyboardButton> row6 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton samarkand = new InlineKeyboardButton("Samarqand");
                samarkand.setCallbackData("samarkand");
                row6.add(samarkand);
                InlineKeyboardButton urgench = new InlineKeyboardButton("Urganch");
                urgench.setCallbackData("urgench");
                row6.add(urgench);

                List<InlineKeyboardButton> row7 = new ArrayList<InlineKeyboardButton>();
                InlineKeyboardButton ferghana = new InlineKeyboardButton("Farg'ona");
                ferghana.setCallbackData("ferghana");
                row7.add(ferghana);
                InlineKeyboardButton khiva = new InlineKeyboardButton("Xiva");
                khiva.setCallbackData("khiva");
                row7.add(khiva);

                min_board.add(row1);
                min_board.add(row2);
                min_board.add(row3);
                min_board.add(row4);
                min_board.add(row5);
                min_board.add(row6);
                min_board.add(row7);

                region_markup.setKeyboard(min_board);

                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Regionlarni tanlang:");
                edit.setReplyMarkup(region_markup);

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            // Ob-havo call_data:start

            if (call_data.equals("jizzakh")) {

                Obhavo o = new Obhavo("Jizzax");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Jizzaxdagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("tashkent")) {

                Obhavo o = new Obhavo("Toshkent");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Toshkentdagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("andijan")) {

                Obhavo o = new Obhavo("Andijon");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Andijondagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("bukhara")) {

                Obhavo o = new Obhavo("Buxoro");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Buxorodagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("gulistan")) {

                Obhavo o = new Obhavo("Guliston");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Gulistondagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("zarafshan")) {


                Obhavo o = new Obhavo("Zarafshon");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Zarafshondagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("karshi")) {


                Obhavo o = new Obhavo("Qarshi");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Qarshidagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("navoi")) {

                Obhavo o = new Obhavo("Navoiy");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Navoiydagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("namangan")) {


                Obhavo o = new Obhavo("Namangan");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Namangandagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("nukus")) {


                Obhavo o = new Obhavo("Nukus");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Nukusdagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("samarkand")) {


                Obhavo o = new Obhavo("Samarqand");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Samarqanddagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("urgench")) {


                Obhavo o = new Obhavo("Urganch");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Urganchdagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("ferghana")) {


                Obhavo o = new Obhavo("Farg'ona");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Farg'onadagi ob-havo: " + o.getHarorat());
                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("khiva")) {


                Obhavo o = new Obhavo("Xiva");
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setText("Xivadagi ob-havo: " + o.getHarorat());

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            //Ob-havo call_data:end

            if (call_data.equals("info")) {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(update.getCallbackQuery().getId());
                answer.setShowAlert(true);
                answer.setText("ID: " + callback_user.getId() + "\nFirst Name: " + callback_user
                        .getFirstName() + "\nLast Name: " + callback_user.getLastName() + "\n" +
                        "Username: " + callback_user.getUserName() + "\n"
                        + "is Bot: " + callback_user.getIsBot());
                edit.setText(answer.getText());
                try {
                    execute(answer);
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("mooncat")) {

                Thread th = new Thread(new Runnable() {
                    int matrix_counter = 0;

                    public void run() {
                        for (int i = 0; i < 3 * matrix.length; i++) {
                            EditMessageText edit = new EditMessageText();
                            edit.setChatId(chat_id);
                            edit.setMessageId((int) message_id);
                            edit.setText(matrix[matrix_counter]);
                            matrix_counter++;
                            if (matrix_counter == 7) {
                                matrix_counter = 0;
                            }
                            try {
                                Thread.sleep(50);
                                execute(edit);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                th.start();

            }

            if (call_data.equals("clicked_natija")) {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                try {
                    edit.setText(getStatus());
                    execute(edit);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("valyuta")) {
                try {

                    String all_text = getCurrent();

                    EditMessageText edit = new EditMessageText();
                    edit.setParseMode(ParseMode.HTML);
                    edit.setMessageId((int) message_id);
                    edit.setChatId(chat_id);
                    edit.setText(all_text);

                    execute(edit);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (call_data.equals("commands")) {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(chat_id);
                edit.setMessageId((int) message_id);
                edit.setParseMode(ParseMode.HTML);
                edit.setText("Guruhda ishlaydigan kommandalar:\n" +
                        "<b>Diqqat❗️ quyidagi funksiyalar faqat bot admin bo'lganda ishlaydi.</b>\n" +
                        "/brt=[satr]   -  Guruh nomini siz bergan satr ga almashtiradi\n" +
                        "/kick   -   Reply qilingan odam ni guruhdan chiqarib tashlaydi.\n" +
                        "/gid   -   Guruh haqida ma`lumot.\n" +
                        "/pin   -   Reply qilingan xabarni pin qiladi.\n" +
                        "/unpin   -   Pin qilingan xabarni olib tashlaydi\n" +
                        "/delete   -   Reply qilingan xabarni o'chirib tashlaydi\n" +
                        "/members_count - Guruhdagi a`zolar sonini hisoblaydi.\n" +
                        "<b>Avtomatik ishlovchi funksiyalar: </b>\n" +
                        "Matnli link va linklarni o'chirib tashlaydi.\nLichkada ishlaydigan kommandalar:\n/id=[id] - telegramdagi shu id egasini topib beradi.");

                try {
                    execute(edit);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
        if (update.getMessage().getNewChatMembers().size() != 0) {
            SendMessage join_ = new SendMessage();
            join_.setChatId(update.getMessage().getChatId());
            join_.setParseMode(ParseMode.MARKDOWN);
            String joiner = "";
            if (update.getMessage().getNewChatMembers().size() > 1)
                for (User u : update.getMessage().getNewChatMembers()) {
                    joiner += u.getFirstName() + ", ";
                }
            else {
                joiner += update.getMessage().getNewChatMembers().get(0).getFirstName();
            }


            join_.setText("Assalomu alaykum*\uD83D\uDC4B" + joiner + "*");

            try {
                execute(join_);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        if (!update.getMessage().getLeftChatMember().getFirstName().equals(null)) {
            SendMessage left_ = new SendMessage(String.valueOf(update.getMessage().getChatId()),
                    "Xayr *" + update.getMessage().getLeftChatMember().
                            getFirstName() + "*");
            left_.setParseMode(ParseMode.MARKDOWN);

            try {
                execute(left_);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private String getBotUsernameAndToken(boolean isUser) {
        Properties p = new Properties();
        try {
            FileReader fr = new FileReader(path_app);
                /*
                In application.properties you have to write properties like below:
                bot.username=<username>
                bot.token=<token>
                instead of <username> and <token> put your special bot's username and token
                 */
            p.load(fr);
        } catch (FileNotFoundException e) {
            System.err.println("File application.properties not found");
        } catch (IOException e) {
            System.err.println("Ma`lumot olishda xatolik");
        }
        if (isUser) {
            return p.getProperty("bot.username");
        } else {
            return p.getProperty("bot.token");
        }
    }

    private String getBotUsernameAndToken() {
        String response = "";
        response = getBotUsernameAndToken(true);
        return response;
    }

    public String getBotUsername() {
        return getBotUsernameAndToken();
    }

    public String getBotToken() {
        return getBotUsernameAndToken(false);
    }

    private boolean inArray(int id, int[] arr) {
        for (int x : arr) {
            if (id == x) return true;
        }
        return false;
    }

    private String getCurrent() {
        try {
            URL url = new URL("http://cbu.uz/uzc/arkhiv-kursov-valyut/xml/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String xml = "";
            BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            while ((line = buffer.readLine()) != null) {
                xml += line + "\n";
            }

            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));

            connection.disconnect();


            int important[] = {784, 156, 978, 392, 410, 417, 398, 643, 840};
            int rates_len = doc.getElementsByTagName("CcyNtry").getLength();
            String date = doc.getElementsByTagName("date").item(0).getTextContent();
            NodeList oz = doc.getElementsByTagName("CcyNm_UZ");//OZ - Lotin
//            NodeList uz = doc.getElementsByTagName("CcyNm_UZC");
//            NodeList ru = doc.getElementsByTagName("CcyNm_RU");
//            NodeList en = doc.getElementsByTagName("CcyNm_EN");
            NodeList rates = doc.getElementsByTagName("Rate");
//            List<String> oz_list = new ArrayList<>();
//            List<String> uz_list = new ArrayList<>();
//            List<String> ru_list = new ArrayList<>();
//            List<String> en_list = new ArrayList<>();
//            List<String> rates_list = new ArrayList<>(); Next Feature

            String all_text = "";

            int id = 0;
            for (int i = 0; i < rates_len; i++) {
                id = Integer.parseInt(doc.getElementsByTagName("CcyNtry").item(i).
                        getAttributes().getNamedItem("ID").getTextContent());
                if (inArray(id, important)) {
                    all_text += oz.item(i).getTextContent() + "\t" + rates.item(i).getTextContent() + " so'm"
                            + "\n";
                }
            }

            all_text += "\n" + date + " ma`lumotlariga ko'ra";

            return all_text;

        } catch (MalformedURLException e) {
            System.err.println("URL da xatolik");
        } catch (IOException e) {
            System.err.println("Kiritish Chiqish tizimida xatolik");
        } catch (ParserConfigurationException e) {
            System.err.println("API da xatolik");
        } catch (SAXException e) {
            System.err.println("API parser exception");
        }

        return "";
    }

}


//http://cbu.uz/uzc/arkhiv-kursov-valyut/xml/ kurs
