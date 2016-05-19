package CommonModel.GameModel.Action;

import CommonModel.GameModel.City.Region;
import Utilities.Class.Constants;
import Utilities.Exception.ActionNotPossibleException;
import Server.Model.Game;
import Server.Model.User;

/**
 * Created by Giulio on 17/05/2016.
 */
public class FastActionChangePermitCardWithHelper extends Action {

    private Region region;

    public FastActionChangePermitCardWithHelper(Region region) {
        this.type = Constants.FAST_ACTION;
        this.region = region;
    }

    /** change the visible permit card spending a helper
     * @param game
     * @param user
     * @throws ActionNotPossibleException
     */
    @Override
    public void doAction(Game game, User user) throws ActionNotPossibleException {
        if (user.getHelpers() > Constants.HELPER_LIMITATION_CHANGE_PERMIT_CARD){
            user.setHelpers(user.getHelpers() - Constants.HELPER_LIMITATION_CHANGE_PERMIT_CARD);
            game.getPermitDeck(region).changePermitCardVisibile();
            removeAction(game, user);
        } else {
            throw new ActionNotPossibleException();
        }
    }

    @Override
    public String toString() {
        return "FastActionChangePermitCardWithHelper{" +
                "region=" + region +
                '}';
    }
}
