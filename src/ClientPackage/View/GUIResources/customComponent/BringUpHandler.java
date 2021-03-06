package ClientPackage.View.GUIResources.CustomComponent;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 * Created by Emanuele on 17/06/2016.
 */
public class BringUpHandler implements EventHandler<MouseEvent> {

    private final Node node;

    public BringUpHandler(Node node) {
        this.node = node;
    }

    @Override
    public void handle(MouseEvent event) {
        this.node.setTranslateY(-3);
    }

    public void setNormalPosition() {
        this.node.setTranslateY(3);
    }
}
