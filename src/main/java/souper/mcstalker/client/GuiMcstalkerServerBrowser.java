package souper.mcstalker.client;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.*;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Environment(EnvType.CLIENT)
public class GuiMcstalkerServerBrowser extends MultiplayerScreen { // 99% copied from MultiplayerScreen, probably needs to get reworked with mixins.
    private static final Logger LOGGER = LogManager.getLogger();
    private final MultiplayerServerListPinger serverListPinger = new MultiplayerServerListPinger();
    private final Screen parent;
    protected MultiplayerServerListWidget serverListWidget;
    private ServerList serverList;
    //private ButtonWidget buttonEdit;
    private ButtonWidget buttonJoin;
    //private ButtonWidget buttonDelete;
    private List<Text> tooltipText;
    private ServerInfo selectedEntry;
    private boolean hasInited;

    private final List<ButtonWidget> drawablesBypass = Lists.newArrayList();

    public GuiMcstalkerServerBrowser(Screen parent, ServerList serverList)
    {
        super(parent);
        this.parent = parent;
        this.serverList = serverList;
        this.hasInited = false;
    }

    @Override
    protected void init()
    {
        client.keyboard.setRepeatEvents(true);
        if (this.hasInited)
        {
            this.serverListWidget.updateSize(width, height, 32, this.height - 64);
        } else
        {
            hasInited = true;
            serverListWidget = new MultiplayerServerListWidget(this, client, width, height, 32, height - 64, 36);

            serverListWidget.setServers(this.serverList);
        }

        drawablesBypass.clear();

        addSelectableChild(serverListWidget);
        buttonJoin = (new ButtonWidget(width / 2 - 154, height - 28, 100, 20, new TranslatableText("selectServer.select"), (button) -> {
            this.connect();
        }));

        drawablesBypass.add(buttonJoin);

        drawablesBypass.add(new ButtonWidget(width / 2 + 4, height - 28, 70, 20, new TranslatableText("selectServer.refresh"), (button) -> {
            this.refresh();
        }));

        drawablesBypass.add(new ButtonWidget(width / 2 + 4 + 76, height - 28, 75, 20, ScreenTexts.CANCEL, (button) -> {
            client.setScreen(parent);
        }));

        int width = 400;

        drawablesBypass.add(new ButtonWidget(this.width / 2 - width / 4, height - 52, width / 2, 20, new LiteralText("Filter Servers"), (button) -> {
            this.client.setScreen(new FilterOptionsScreen(new LiteralText("Filter Options"), this.parent));
        }));

        for(ButtonWidget widget : drawablesBypass) // cheap hack
        {
            addDrawableChild(widget);
        }

        updateButtonActivationStates();
    }

    @Override
    public void tick() {
        this.serverListPinger.tick();
    }

    @Override
    public void removed()
    {
        client.keyboard.setRepeatEvents(false);
        this.serverListPinger.cancel();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (super.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        } else if (keyCode == 294)
        {
            this.refresh();
            return true;
        } else if (this.serverListWidget.getSelectedOrNull() != null)
        {
            if (keyCode != 257 && keyCode != 335)
            {
                return this.serverListWidget.keyPressed(keyCode, scanCode, modifiers);
            } else
            {
                this.connect();
                return true;
            }
        } else
        {
            return false;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta)
    {

        this.tooltipText = null;
        //this.renderBackground(matrices);
        this.serverListWidget.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, "MCStalker Server Browser", this.width / 2, 20, 16777215);
        for(ButtonWidget widget : drawablesBypass)
        {
            widget.render(matrices, mouseX, mouseY, delta);
        }
        if (this.tooltipText != null)
        {
            this.renderTooltip(matrices, this.tooltipText, mouseX, mouseY);
        }
    }

    @Override
    public void connect()
    {
        MultiplayerServerListWidget.Entry entry = (MultiplayerServerListWidget.Entry)serverListWidget.getSelectedOrNull();
        if (entry instanceof MultiplayerServerListWidget.ServerEntry)
        {
            this.connect(((MultiplayerServerListWidget.ServerEntry)entry).getServer());
        } else if (entry instanceof MultiplayerServerListWidget.LanServerEntry)
        {
            LanServerInfo lanServerInfo = ((MultiplayerServerListWidget.LanServerEntry)entry).getLanServerEntry();
            this.connect(new ServerInfo(lanServerInfo.getMotd(), lanServerInfo.getAddressPort(), true));
        }

    }


    private void connect(ServerInfo entry)
    {
        ConnectScreen.connect(this, client, ServerAddress.parse(entry.address), entry);
    }

    @Override
    public void select(MultiplayerServerListWidget.Entry entry)
    {
        this.serverListWidget.setSelected(entry);
        this.updateButtonActivationStates();
    }

    @Override
    protected void updateButtonActivationStates()
    {
        this.buttonJoin.active = false;

        MultiplayerServerListWidget.Entry entry = (MultiplayerServerListWidget.Entry)this.serverListWidget.getSelectedOrNull();
        if (entry != null && !(entry instanceof MultiplayerServerListWidget.ScanningEntry))
        {
            this.buttonJoin.active = true;
            if (entry instanceof MultiplayerServerListWidget.ServerEntry)
            {
                // removed
            }
        }

    }

    @Override
    public MultiplayerServerListPinger getServerListPinger() {
        return this.serverListPinger;
    }

    @Override
    public void setTooltip(List<Text> tooltipText) {
        this.tooltipText = tooltipText;
    }

    @Override
    public ServerList getServerList() {
        return this.serverList;
    }

    private void refresh()
    {
        this.client.setScreen(new GuiMcstalkerServerBrowser(this.parent, this.serverList));
    }
}
