package CommonModel.GameModel.Bonus.SingleBonus;

import CommonModel.GameModel.Bonus.Generic.Bonus;
import Utilities.Class.Constants;
import Utilities.Exception.ActionNotPossibleException;
import Server.Model.Game;
import Server.Model.User;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Giulio on 13/05/2016.
 */
public class PoliticCardBonus implements Bonus, Serializable {

    @Override
    public void getBonus(User user, Game game) throws ActionNotPossibleException {
        user.addPoliticCard(game.getPoliticCards().drawACard());
    }

    @Override
    public String getBonusName() {
        return "PoliticCardBonus";
    }

    @Override
    public ArrayList<Bonus> getBonusArrayList() {
        return null;
    }

    @Override
    public String getBonusURL() {
        return Constants.IMAGE_PATH+"RetroPoliticCard.png";
    }

    @Override
    public String getBonusInfo() {
        return null;
    }
}
