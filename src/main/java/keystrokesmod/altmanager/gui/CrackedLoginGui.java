package keystrokesmod.altmanager.gui;

import java.io.IOException;

import keystrokesmod.altmanager.Alt;
import keystrokesmod.altmanager.AltManagerGui;
import keystrokesmod.altmanager.AccountData;
import keystrokesmod.altmanager.SessionChanger;
import keystrokesmod.altmanager.util.AltJsonHandler;
import keystrokesmod.altmanager.util.NameGenerator;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class CrackedLoginGui extends GuiScreen {
    private AltManagerGui parent;
    private GuiButton loginButton, backButton, randomButton;
    private GuiTextField usernameField;

    public CrackedLoginGui(AltManagerGui parent) {
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
        this.usernameField = new GuiTextField(0, this.fontRendererObj, centerX - (fieldWidth / 2), baseY, fieldWidth, fieldHeight);
        this.usernameField.setMaxStringLength(14);
        
        this.loginButton = new GuiButton(0, centerX - (buttonWidth / 2), baseY + fieldHeight + 35, buttonWidth / 2, buttonHeight, "Login Cracked");
        this.backButton = new GuiButton(1, centerX, baseY + fieldHeight + 35, buttonWidth / 2, buttonHeight, "Back");
        this.randomButton = new GuiButton(2, centerX - (buttonWidth / 2), baseY + fieldHeight + 8, buttonWidth, buttonHeight, "Generate Random");
        
        this.buttonList.add(loginButton);
        this.buttonList.add(backButton);
        this.buttonList.add(randomButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "Cracked Login", this.width / 2, 20, 0xFFFFFF);
        this.usernameField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
        	if(!usernameField.getText().equals("")) {
        		String username = usernameField.getText();
        		Alt alt = new Alt(null, null, username, true);
        		boolean isBanned = AccountData.isBanned(username);
        		alt.setBanned(isBanned);
        		parent.alts.add(alt);
        		SessionChanger.instance().loginCracked(username);
                AltJsonHandler.start();
        		AltJsonHandler.saveAlts();
                AltJsonHandler.loadAlts();
                AltManagerGui.status = "§aAdded cracked alt " + username;
        		this.mc.displayGuiScreen(parent);
        	}
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(parent);
        } else if (button.id == 2) {
            usernameField.setText(NameGenerator.generateRandomUsername());
        }
    }
    
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.usernameField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }


}

