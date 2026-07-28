package keystrokesmod.altmanager.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import keystrokesmod.altmanager.Alt;
import keystrokesmod.altmanager.AltManagerGui;
import keystrokesmod.altmanager.SessionChanger;
import keystrokesmod.altmanager.util.AltJsonHandler;
import keystrokesmod.utility.Reflection;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;

public class MicrosoftLoginGui extends GuiScreen{
	private AltManagerGui parent;
	private GuiButton loginButton, backButton;
	private GuiTextField tokenField;
	
	public MicrosoftLoginGui(AltManagerGui parent) {
		this.parent = parent;
	}
	
	@Override
    public void initGui() {
        int centerX = this.width / 2;
        int fieldWidth = 150;
        int fieldHeight = 20;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int baseY = this.height / 2 - 20;
        
        this.buttonList.clear();
        this.tokenField = new GuiTextField(0, this.fontRendererObj, centerX - (fieldWidth / 2), baseY, fieldWidth, fieldHeight);
        this.tokenField.setMaxStringLength(32767);
        this.loginButton = new GuiButton(0, centerX - (buttonWidth / 2), baseY + fieldHeight + 10, buttonWidth, buttonHeight, "Login");
        this.backButton = new GuiButton(1, centerX - (buttonWidth / 2), baseY + fieldHeight + 40, buttonWidth, buttonHeight, "Back");
        
        this.buttonList.add(loginButton);
        this.buttonList.add(backButton);
    }
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "Token Login", this.width / 2, 20, 0xFFFFFF);
        this.tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
	
	@Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
        	loginWithToken(tokenField.getText());
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(parent);
        }
        super.actionPerformed(button);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.tokenField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.tokenField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    private void loginWithToken(String token) {
        new Thread(() -> {
            try {
                String[] playerInfo = getProfileInfo(token);
                Session newSession = new Session(playerInfo[0], playerInfo[1], token, "mojang");
                try {
                    if (Reflection.session != null) {
                        Reflection.session.set(mc, newSession);
                        mc.addScheduledTask(() -> AltManagerGui.status = "§aLogged in as " + newSession.getUsername());

                        mc.addScheduledTask(() -> {
                            // Upsert token-based account and persist
                            Alt existingAlt = null;
                            for (Alt alt : AltManagerGui.alts) {
                                if (alt.getName().equals(newSession.getUsername()) ||
                                        (alt.getUuid() != null && alt.getUuid().equals(newSession.getPlayerID()))) {
                                    existingAlt = alt;
                                    break;
                                }
                            }
                            if (existingAlt != null) {
                                existingAlt.setUuid(newSession.getPlayerID());
                                existingAlt.setRefreshToken(token);
                                existingAlt.setBanned(keystrokesmod.altmanager.AccountData.isBanned(newSession.getUsername()));
                            } else {
                                Alt alt = new Alt(newSession.getUsername(), "", newSession.getUsername(), false);
                                alt.setUuid(newSession.getPlayerID());
                                alt.setRefreshToken(token);
                                alt.setBanned(keystrokesmod.altmanager.AccountData.isBanned(newSession.getUsername()));
                                AltManagerGui.alts.add(alt);
                            }

                            AltJsonHandler.start();
                            AltJsonHandler.saveAlts();
                            AltJsonHandler.loadAlts();

                            AltManagerGui.status = "§aLogged in as " + newSession.getUsername();
                            this.mc.displayGuiScreen(parent);
                        });
                    } else {
                        mc.addScheduledTask(() -> AltManagerGui.status = "§cSession field not initialized");
                    }
                } catch (Exception e) {
                    mc.addScheduledTask(() -> AltManagerGui.status = "§cFailed to set session");
                    e.printStackTrace();
                }
            }catch (Exception e) {
                mc.addScheduledTask(() -> AltManagerGui.status = "§cFailed login");
                e.printStackTrace();
            }
        }).start();
    }

    private String[] getProfileInfo(String token) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            request.setHeader("Authorization", "Bearer " + token);
            try (CloseableHttpResponse response = client.execute(request)) {
                String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(jsonString).getAsJsonObject();
                String username = json.get("name").getAsString();
                String uuid = json.get("id").getAsString();
                return new String[]{username, uuid};
            }
        }
    }
}

