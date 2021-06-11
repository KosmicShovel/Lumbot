package slaynash.lum.bot.discord;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;

public class ServerMessagesHandler {

    private static final Map<Long, long[]> whitelistedRolesServers = new HashMap<>() {{
        put(439093693769711616L /* VRCMG */, new long[] {
                631581319670923274L /* Staff */,
                662720231591903243L /* Helper */,
                825266051277258754L /* cutie */,
                673725166626406428L /* Modder */ });
        put(600298024425619456L /* emmVRC */, new long[] {
                748392927365169233L /* Admin */,
                653722281864069133L /* Helper */ });
        put(663449315876012052L /* MelonLoader */, new long[] {
                663450403194798140L /* Lava Gang */,
                663450567015792670L /* Administrator */,
                663450611253248002L /* Moderator */,
                663450655775522843L /* Modder */ });
        put(673663870136746046L /* Modders & Chill */, new long[] {
                673725166626406428L /* Modders */,
                673726384450961410L /* Moderators */ });
    }};

    private static final String[] alreadyHelpedSentences = new String[] {
        "I already answered you <:konataCry:553690186022649858>",
        "Why won't you read my answer <:angry:835647632843866122>",
        "There's already the answer up here!! <:cirHappy:829458722634858496>",
        "I've already given you a response! <:MeguminEyes:852057834686119946>"
    };

    private static final String[] alreadyHelpedSentencesRare = new String[] {
        "I wish I wasn't doing this job sometimes <:02Dead:835648208272883712>",
        "https://cdn.discordapp.com/attachments/657545944136417280/836231859998031932/unknown.png",
        "Your literacy skills test appears to have failed you. <:ram_disgusting:828070759070695425>",
        "<https://lmgtfy.app/?q=How+do+I+read>"
    };
    
    private static final String[] thankedSentences = new String[] {
        "You're Welcome <:Neko_cat_kiss_heart:851934821080367134>",
        "<:cirHappy:829458722634858496>",
        "Anytime <:Neko_cat_kiss_heart:851934821080367134>",
        "Always happy to help!",
        "Mhm of course!",
        "No problem!",
        "Glad I could help!"
    };

    private static final String[] thankedSentencesRare = new String[] {
        "Notices you senpai <:cirHappy:829458722634858496>",
        "https://tenor.com/view/barrack-obama-youre-welcome-welcome-gif-12542858"
    };

    private static final String[] helloLum = new String[] {
        "<:Neko_cat_owo:851938214105186304>",
        "<:Neko_cat_shrug:851938033724817428>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_wave:851938087353188372>"
    };

    private static final String[] niceLum = new String[] {
        "<:Neko_cat_donut:851936309718024203>",
        "<:Neko_cat_okay:851938634327916566>",
        "<:Neko_cat_pizza:851935753826205736>",
        "<:Neko_cat_royal:851935888178544660>",
        "<:Neko_cat_woah:851935805874110504>",
        "<a:Neko_cat_HeadPat:851934772959510578>",
        "<a:HeartCat:828087151232286749>"
    };

    private static final String[] badLum = new String[] {
        "<:Neko_cat_drool_stupid:851936505516785715>",
        "<:Neko_cat_fear:851936400819486720>",
        "<:Neko_cat_prison:851936449548255264>"
    };

    private static final String[] gunLum = new String[] {
        "<:Neko_cat_Gun:851934721914175498>"
    };

    private static final int helpDuration = 6 * 60; //in seconds

    private static Random random = new Random();
    private static String fileExt;
    
    private static List<HelpedRecentlyData> helpedRecently = new ArrayList<>();

    public static void handle(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;

        System.out.printf("[%s] [%s][%s] %s: %s\n",
                TimeManager.getTimeForLog(),
                event.getGuild().getName(),
                event.getTextChannel().getName(),
                event.getAuthor().getName(),
                event.getMessage().getContentRaw() );

        if (!checkDllPostPermission(event)) {
            event.getMessage().delete().queue();
            event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("<@!" + event.getMessage().getMember().getId() + "> tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
            return;
        }

        if (event.getGuild().getIdLong() == 663449315876012052L) {
            String messageLowercase = event.getMessage().getContentRaw().toLowerCase();
            if (messageLowercase.contains("melonclient") || messageLowercase.contains("melon client") || messageLowercase.contains("tlauncher"))
                event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
        }
    
        CommandManager.runAsServer(event);

        String message = event.getMessage().getContentRaw().toLowerCase();
        
        if (message.contains("[error]") || message.contains("developer:") || message.contains("[internal failure]")) {
            System.out.println("Log was typed");
            
            boolean postedInWhitelistedServer = false;
            boolean isStaff = false;
            long guildId = event.getGuild().getIdLong();
            for (long whitelistedGuildId : whitelistedRolesServers.keySet()) {
                if (whitelistedGuildId == guildId) {
                    postedInWhitelistedServer = true;
                    break;
                }
            }
            if(postedInWhitelistedServer) {
                for (Entry<Long, long[]> whitelistedRolesServer : whitelistedRolesServers.entrySet()) {
                    Guild targetGuild;
                    Member serverMember;
                    if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                        (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                    
                        List<Role> roles = serverMember.getRoles();
                        for (Role role : roles) {
                            long roleId = role.getIdLong();
                            for (long whitelistedRoleId : whitelistedRolesServer.getValue()) {
                                if (whitelistedRoleId == roleId) {
                                    isStaff = true; // The sender is whitelisted
                                    System.out.println("Was Staff, allowing post");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (postedInWhitelistedServer && !isStaff) {
                event.getChannel().sendMessage("<@!" + event.getMessage().getMember().getId() + "> Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                event.getMessage().delete().queue();
            }
        }

        if (message.contains("meap.gg") || message.contains("pqzmexys")) {
            System.out.println("Notorious link detected");
            event.getMessage().delete().queue();
            String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong()); // https://discord.com/channels/663449315876012052/663461849102286849/801676270974795787
            if (reportChannel != null) {
                event.getGuild().getTextChannelById(reportChannel).sendMessage(
                        JDAManager.wrapMessageInEmbed(
                                "User <@" + event.getMember().getId() + "> Posted a link to Notorious and message has been deleted.",
                                Color.RED)).queue();
            }
        }

        else if (message.matches("(.*\\b(good|nice|love|cool|cutie|helped)\\b.*) (.*\\blum\\b.*)|(.*\\blum\\b.*) (.*\\b(good|nice|love|cool|cutie|helped)\\b.*)")) {
            System.out.println("Nice Lum was detected");
            event.getChannel().sendMessage(niceLum[random.nextInt(niceLum.length)]).queue();
        }

        else if (message.contains(/*f*/" off lum") || message.contains(/*f*/" you lum")) {
            System.out.println("F off Lum was detected");
            event.getChannel().sendMessage(gunLum[random.nextInt(gunLum.length)]).queue();
        }

        else if (message.contains("bad lum") || message.contains("lum shush") || message.contains(/*shut*/" up lum") || message.contains(/*shush*/" it lum")) {
            System.out.println("Bad Lum was detected");
            event.getChannel().sendMessage(badLum[random.nextInt(badLum.length)]).queue();
        }

        else if (message.contains("hello lum") || message.contains("hi lum")) {
            System.out.println("Hello Lum was detected");
            event.getChannel().sendMessage(helloLum[random.nextInt(helloLum.length)]).queue();
        }

        else if (message.contains("thank") || message.contains("thx") || message.contains("neat") || message.contains("cool") || message.contains("nice") ||
                message.contains("helpful") || message.contains("epic") || message.contains("worked") || message.contains("tysm") || message.equals("ty") ||
                message.contains(" ty ") || message.contains("fixed") || message.matches("(^|.*\\s)rad(.*)") || message.contains("that bot") ||
                message.contains("this bot") || message.contains("awesome") || message.contains(" wow ")) {
            System.out.println("Thanks was detected");
            if (wasHelpedRecently(event) && (event.getMessage().getReferencedMessage()==null || event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 275759980752273418L/*LUM*/)) {
                String sentence;
                boolean rare = random.nextInt(100) == 69;
                if (rare)
                    sentence = "You're Welcome, but thank <@145556654241349632> and <@240701606977470464> instead for making me. <a:HoloPet:829485119664160828>";
                else {
                    rare = random.nextInt(10) == 9;
                    sentence = rare
                    ? thankedSentencesRare[random.nextInt(thankedSentencesRare.length)]
                    : thankedSentences    [random.nextInt(thankedSentences.length)];
                }
                event.getChannel().sendMessage(sentence).queue();
            }
        }
        
        else if (message.contains("help") && !message.contains("helping") || message.contains("fix") || message.contains("what do "/*i do*/) || message.contains("what should "/*i do*/)) {
            System.out.println("Help was detected");
            if (wasHelpedRecently(event) && (event.getMessage().getReferencedMessage()==null || event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 275759980752273418L/*LUM*/)) {
                String sentence;
                boolean rare = random.nextInt(1000) == 420;
                if (rare)
                    sentence = "Shut the fuck up, I literally answered your dumb ass!";
                else {
                    rare = random.nextInt(10) == 9;
                    sentence = rare
                    ? alreadyHelpedSentencesRare[random.nextInt(alreadyHelpedSentencesRare.length)]
                    : alreadyHelpedSentences    [random.nextInt(alreadyHelpedSentences.length)];
                }
                event.getChannel().sendMessage(sentence).queue();
            }
        }
        
        else if ((message.contains("credit") || message.contains("stole")) && message.contains("lum")) {
            System.out.println("Lum stole Credit");
            event.getChannel().sendMessage("<:Hehe:792738744057724949>").queue();
        }
        
        new Thread(() -> {
            try {
                MelonScanner.scanMessage(event);
            }
            catch(Exception e) {
                e.printStackTrace();
                
                String error = "**An error has occured while reading logs:**\n" + ExceptionUtils.getStackTrace(e);
                
                if (error.length() > 1000) {
                    String[] lines = error.split("\n");
                    String toSend = "";
                    int i = 0;
                    
                    while (i < lines.length) {
                        if ((toSend + lines[i] + 1).length() > 1000) {
                            toSend += "...";
                            break;
                        }
                        
                        toSend += "\n" + lines[i];
                    }
                    
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(toSend, Color.RED)).queue();
                }
                else
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(error, Color.RED)).queue();
            }
        }).start();
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL
     * @param event
     * @return true if the message is posted in a guild using a whitelist, contains a DLL attachment, and isn't posted by a whitelisted user
     */
    private static boolean checkDllPostPermission(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        boolean postedInWhitelistedServer = false;
        for (long whitelistedGuildId : whitelistedRolesServers.keySet()) {
            if (whitelistedGuildId == guildId) {
                postedInWhitelistedServer = true;
                break;
            }
        }
        
        if (!postedInWhitelistedServer)
            return true; // Not a whitelisted server
        
        for (Attachment attachment : event.getMessage().getAttachments()) {
            fileExt = attachment.getFileExtension();
            if (fileExt == null) fileExt = "";
            fileExt = fileExt.toLowerCase();
            
            if (fileExt.equals("dll") || fileExt.equals("exe") || fileExt.equals("zip") || fileExt.equals("7z") || fileExt.equals("rar") || fileExt.equals("unitypackage") || fileExt.equals("vrca") || fileExt.equals("fbx")) {

                for (Entry<Long, long[]> whitelistedRolesServer : whitelistedRolesServers.entrySet()) {
                    Guild targetGuild;
                    Member serverMember;
                    if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                        (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                        
                        List<Role> roles = serverMember.getRoles();
                        for (Role role : roles) {
                            long roleId = role.getIdLong();
                            for (long whitelistedRoleId : whitelistedRolesServer.getValue())
                                if (whitelistedRoleId == roleId)
                                    return true; // The sender is whitelisted
                        }
                    }
                    
                }

                return false; // The sender isn't allowed to send a DLL file
            }
        }

        return true; // No attachement, or no DLL
    }
    
    public static void addNewHelpedRecently(MessageReceivedEvent event) {
        for (int i = helpedRecently.size() - 1; i >= 0; --i)
            if (helpedRecently.get(i).time + helpDuration < Instant.now().getEpochSecond())
                helpedRecently.remove(i);
        
        helpedRecently.add(new HelpedRecentlyData(event.getMember().getIdLong(), event.getChannel().getIdLong()));
        System.out.println("Helped recently added");
    }
    
    public static boolean wasHelpedRecently(MessageReceivedEvent event) {
        for (int i = 0; i < helpedRecently.size(); ++i) {
            HelpedRecentlyData hrd = helpedRecently.get(i);
            if (hrd.channelid == event.getChannel().getIdLong() && hrd.userid == event.getMember().getIdLong() && hrd.time + helpDuration > Instant.now().getEpochSecond()) {
                helpedRecently.remove(i); // trigger only one message per log
                return true;
            }
        }
        
        return false;
    }
}
