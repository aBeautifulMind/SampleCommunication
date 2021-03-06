package Server.Controller;

import CommonModel.GameModel.Action.Action;
import CommonModel.GameModel.Bonus.Generic.Bonus;
import CommonModel.GameModel.Bonus.SingleBonus.NobilityBonus;
import CommonModel.GameModel.Card.Deck.PermitDeck;
import CommonModel.GameModel.Card.SingleCard.PermitCard.PermitCard;
import CommonModel.GameModel.Card.SingleCard.PoliticCard.PoliticCard;
import CommonModel.GameModel.City.City;
import CommonModel.GameModel.City.RegionName;
import CommonModel.GameModel.Council.Helper;
import CommonModel.GameModel.Council.King;
import CommonModel.GameModel.Market.BuyableWrapper;
import CommonModel.Snapshot.BaseUser;
import CommonModel.Snapshot.SnapshotToSend;
import CommonModel.Snapshot.UserColor;
import Server.Model.FakeUser;
import Server.Model.Game;
import Server.Model.Map;
import Server.Model.User;
import Utilities.Class.Constants;
import Utilities.Class.InternalLog;
import Utilities.Exception.ActionNotPossibleException;
import Utilities.Exception.AlreadyPresentException;
import Utilities.Exception.MapsNotFoundException;

import java.io.Serializable;
import java.util.*;

/** The Game Controllers and the class that contains the logic of the game.
 * Created by Emanuele on 13/05/2016.
 */
public class GameController implements Serializable {

    private Game game;
    private TimerTask timerTask;
    private Timer timer;
    private int duration = Constants.GAME_TIMEOUT;
    private ArrayList<Map> availableMaps = new ArrayList<>();
    // initialized to users size, when 0 start market
    private int turnCounter = 0;
    private HashMap<User, Boolean> marketHashMap = new HashMap<>();
    private ArrayList<User> users = new ArrayList<>();
    private boolean sellPhase = false;
    private boolean buyPhase = false;
    private int nextUser;
    private int lastUser = -1;
    private Timer roundTimer = new Timer();
    private UserColor[] userColorSet;
    private FakeUser fakeUser;

    /** Constructor used for serialization
     */
    public GameController() {
    }

    /** Constructor
     * @param game is the game that is controlled
     */
    public GameController(Game game) {
        this.game = game;
        this.timer = new Timer();
        try {
            availableMaps = Map.readAllMap();
        } catch (MapsNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Starts first timer of the login users
     */
    public void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                notifyStarted();
            }
        };
    }

    /** Notify that the game is started
     */
    public void notifyStarted() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (game.getUsers().size() == 2) {
            creatingFakeUser();
        }
        users = new ArrayList<>(game.getUsers());
        game.setStarted(true);
        setDefaultStuff();
        // send map to first user
        for (User user : users) {
            if (user.isConnected()) {
                sendAvailableMap(user);
                break;
            }
        }
        startConnectedTimer();
    }

    /** Creates the strange rule in case of a game composed by two players
     */
    private void creatingFakeUser() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        fakeUser = new FakeUser();
        game.getUsersInGame().put(fakeUser.getUsername(), fakeUser);
        fakeUser.setUsername("FakeUser");
    }

    /** Set the configuration for two players
     */
    private void configurationForTwoPlayers() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<PermitCard> permitCardArray = new ArrayList<>();
        for (java.util.Map.Entry<RegionName, PermitDeck> permitDeck : game.getPermitDecks().entrySet()) {
            permitCardArray.add(permitDeck.getValue().getAndRemoveRandomPermitCard());
        }
        for (PermitCard permitCard : permitCardArray) {
            for (Character character : permitCard.getCityAcronimous()) {
                for (City city : game.getMap().getCity()) {
                    if (city.getCityName().getCityName().startsWith(character.toString().toUpperCase()) && !fakeUser.getUsersEmporium().contains(city))
                        fakeUser.addEmporium(city);
                }
            }
        }

    }

    /** Set the first configuration of the game, it is the init
     */
    private void setDefaultStuff() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        int userCounter = 0;
        for (User user : users) {
            userColorSet = UserColor.values();
            user.setUserColor(colorAvailable());
            if (!(user instanceof FakeUser)) {
                user.setHelpers(Constants.DEFAULT_HELPER_COUNTER + userCounter);
                user.setCoinPathPosition(Constants.FIRST_INITIAL_POSITION_ON_MONEY_PATH + userCounter);
                user.setNobilityPathPosition(game.getNobilityPath().getPosition()[Constants.INITIAL_POSITION_ON_NOBILITY_PATH]);
                user.setVictoryPathPosition(Constants.INITIAL_POSITION_ON_VICTORY_PATH);
                userCounter++;
            }
            ArrayList<PoliticCard> politicCardArrayList = new ArrayList<>();
            for (int cont = 0; cont < Constants.DEFAULT_POLITIC_CARD_HAND; cont++) {
                politicCardArrayList.add(game.getPoliticCards().drawACard());
            }
            user.setPoliticCards(politicCardArrayList);
        }
    }

    /** Are the available color that can be choosen for the user
     * @return
     */
    private UserColor colorAvailable() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<UserColor> shuffledUserColor = new ArrayList<>(Arrays.asList(UserColor.values()));
        Collections.shuffle(shuffledUserColor);
        for (UserColor userColor : shuffledUserColor) {
            boolean found = false;
            for (User user : game.getUsersInGame().values()) {
                if (user.getUserColor() != null && user.getUserColor().equals(userColor)) {
                    found = true;
                }
            }
            if (!found) {
                return userColor;
            }
        }
        return null;
    }

    /** Cancel the timeout and start game
     */
    public void cancelTimeout() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        timer.cancel();
    }

    /** Set the timeout
     */
    public void setTimeout() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (timer == null) {
            timer = new Timer();
        } else {
            timer.cancel();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    notifyStarted();
                }
            };
            timer = new Timer();
        }
        timer.schedule(timerTask, duration);
    }

    /** Create snapshot and change round
     * @param user user that has finished round
     */
    public void onFinishRound(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        cancelTimer();
        turnCounter--;
        user.getBaseCommunication().finishTurn();
        for (int cont = 0; cont < users.size(); cont++) {
            if (user.equals(users.get(cont))) {
                nextUser = cont + 1;
                while (!users.get((nextUser) % game.getUsers().size()).isConnected() && nextUser % game.getUsers().size() != cont) {
                    System.out.println("User not connected: " + users.get((nextUser) % game.getUsers().size()));
                    turnCounter--;
                    nextUser++;
                }
                if (lastUser != nextUser % game.getUsers().size()) {
                    if ((nextUser % game.getUsers().size()) == cont) {
                        startMarket();
                    } else {
                        if (turnCounter <= 0) {
                            startMarket();
                        } else {
                            changeRound(nextUser);
                        }
                    }
                } else {
                    checkUserWhoWin();
                }
            }
        }
    }

    /** Cancel the timer and changes round
     */
    private void cancelTimer() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (roundTimer != null) {
            roundTimer.cancel();
        }
    }

    /** Initialize the changing round
     * @param nextUser is the next user that plays
     */
    private void changeRound(int nextUser) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<User> userArrayList = new ArrayList<>(game.getUsers());
        userArrayList.get((nextUser) % game.getUsers().size()).setMainActionCounter(Constants.MAIN_ACTION_POSSIBLE);
        userArrayList.get((nextUser) % game.getUsers().size()).setFastActionCounter(Constants.FAST_ACTION_POSSIBLE);
        userArrayList.get((nextUser) % game.getUsers().size()).drawCard();
        userArrayList.get((nextUser) % game.getUsers().size()).getBaseCommunication().changeRound();
        sendSnapshotToAll();
        startRoundTimer(userArrayList.get((nextUser) % game.getUsers().size()));
    }

    /** Starts the round timer
     * @param user is the user where is set the timer
     */
    private void startRoundTimer(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        roundTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                user.getBaseCommunication().ping();
                onUserPass(user);
            }
        };
        roundTimer.schedule(timerTask, Constants.ROUND_DURATION);
    }

    /** Starts the market phase
     */
    private void startMarket() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        sellPhase = true;
        marketHashMap.clear();
        users.forEach(user -> {
            Runnable runnable = () -> {
                sendStartMarket(user);
            };
            new Thread(runnable).start();

        });
    }

    /** Starts the timer for check communication in RMI
     */
    private void startConnectedTimer() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Timer checkUserTimer = new Timer();
        TimerTask checkUserTimerTask = new TimerTask() {
            @Override
            public void run() {
                for (Iterator<User> iterator = users.iterator(); iterator.hasNext(); ) {
                    try {
                        iterator.next().getBaseCommunication().ping();
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        };
        checkUserTimer.scheduleAtFixedRate(checkUserTimerTask, 0, 30000);
    }

    /** Send that market is started
     * @param user is the last user where starts market
     */
    private void sendStartMarket(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        user.getBaseCommunication().sendStartMarket();
    }

    /** Do the action in the server
     * @param action is the action to do
     * @param user is the user that has done the action
     * @throws ActionNotPossibleException is the exception
     */
    public void doAction(Action action, User user) throws ActionNotPossibleException {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        action.doAction(game, user);
    }

    /** Send available map to client
     * @param user is the user that can choose the map
     */
    private void sendAvailableMap(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        user.getBaseCommunication().sendAvailableMap(availableMaps);
    }

    /** Set map and init game called by client
     * @param map is the map choosen and that must be set into the game
     */
    public void setMap(Map map) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (availableMaps.contains(map)) {
            Map mapToFind = findMap(map);
            game.setMap(mapToFind);
            game.setKing(new King(map.getCity().get(0), game.getBank()));
            setKingPosition(game, mapToFind);
            for (User user : game.getUsers()) {
                SnapshotToSend snapshotToSend = new SnapshotToSend(game, user);
                // init game
                user.getBaseCommunication().sendSelectedMap(snapshotToSend);
            }
            sendFinishMarketToAll();
            //count true user
            long trueUser = game.getUsers().stream().filter(user -> !(user instanceof FakeUser)).count();

            if (trueUser == 2) {
                configurationForTwoPlayers();
            }
            selectFirstPlayer();

            //TODO: READD TEN EMPORIUMS
            //addTenEmporiums();
        } else {
            System.out.println("MAP NOT PRESENT");
        }
    }

    /** Method that action the finish game and add 9 emporia
     */
    private void addTenEmporiums() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        for (User user : users) {
            int cont = 0;
            for (City city : game.getMap().getCity()) {
                if (cont < 9) {
                    if (!(user instanceof FakeUser)) {
                        user.addEmporium(city);
                        cont++;
                    }
                }
            }
        }
    }

    /** Set the initial king position
     * @param game is the game where put king
     * @param mapToFind is the map received
     */
    private void setKingPosition(Game game, Map mapToFind) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        for (City city : mapToFind.getCity()) {
            if (city.getColor().equals(CommonModel.GameModel.City.Color.PURPLE)) {
                game.setKing(new King(city, game.getBank()));
                break;
            }
        }
    }

    /** Check if the map exist
     * @param map is the map send
     * @return te mapSelected
     */
    private Map findMap(Map map) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        for (Map mapToSelect : availableMaps) {
            if (map.equals(mapToSelect)) {
                return mapToSelect;
            }
        }
        return null;
    }

    /** Disable market phase in all user
     */
    private synchronized void sendFinishMarketToAll() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        new Thread(() -> {
            for (User user : users) {
                user.getBaseCommunication().disableMarketPhase();
            }
        }).start();
    }

    /** Select the first player in game, round start from that user
     */
    private void selectFirstPlayer() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<User> users = new ArrayList<>(game.getUsers());
        turnCounter = users.size();
        // select first user
        for (User user : users) {
            if (user.isConnected()) {
                changeRound(users.indexOf(user));
                nextUser = users.indexOf(user);
                break;
            }
        }
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).isConnected() && i != nextUser)
                users.get(i).getBaseCommunication().finishTurn();
        }
        sendSnapshotToAll();
    }

    /** Send the snapshot to all the users in game
     */
    public synchronized void sendSnapshotToAll() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        new Thread(() -> {
            for (User user : game.getUsers()) {
                if (user.isConnected()) {
                    SnapshotToSend snapshotToSend = new SnapshotToSend(game, user);
                    user.getBaseCommunication().sendSnapshot(snapshotToSend);
                }
            }
        }).start();
    }

    /** Called by client when receive an object to sell
     * @param buyableWrappers are the object that are buyable
     * @return that has insert into the buyable list the objects
     */
    public boolean onReceiveBuyableObject(ArrayList<BuyableWrapper> buyableWrappers) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        for (BuyableWrapper buyableWrapper : buyableWrappers) {
            try {
                game.addBuyableWrapper(buyableWrapper);
            } catch (AlreadyPresentException e) {
            }
        }
        return true;
    }

    /** Called by client when receive object to buy
     * @param user is the user that buy
     * @param buyableWrappers are the object buyable
     * @return if is possible to buy
     */
    public boolean onBuyObject(User user, ArrayList<BuyableWrapper> buyableWrappers) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        int counter = 0;
        for (BuyableWrapper buyableWrapper : buyableWrappers) {
            try {
                game.getMoneyPath().goAhead(user, -buyableWrapper.getCost());
                game.getMoneyPath().goAhead(game.getUser(buyableWrapper.getUsername()), buyableWrapper.getCost());
                game.removeFromMarketList(buyableWrapper);
                if (buyableWrapper.getBuyableObject() instanceof PermitCard) {
                    game.getUser(buyableWrapper.getUsername()).removePermitCardDefinitevely((PermitCard) buyableWrapper.getBuyableObject());
                    user.addPermitCard((PermitCard) buyableWrapper.getBuyableObject());
                } else if (buyableWrapper.getBuyableObject() instanceof PoliticCard) {
                    game.getUser(buyableWrapper.getUsername()).removePoliticCard((PoliticCard) buyableWrapper.getBuyableObject());
                    user.addPoliticCard((PoliticCard) buyableWrapper.getBuyableObject());
                } else if (buyableWrapper.getBuyableObject() instanceof Helper) {
                    game.getUser(buyableWrapper.getUsername()).removeHelper((Helper) buyableWrapper.getBuyableObject());
                    user.addHelper();
                }
                counter++;
            } catch (ActionNotPossibleException e) {
            }
        }
        sendSnapshotToAll();
        return counter == buyableWrappers.size();
    }

    /** Remove from market the item
     * @param item item that must be removed
     */
    public synchronized void onRemoveItem(BuyableWrapper item) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        game.removeFromMarketList(item);
        sendSnapshotToAll();
    }

    /** Finish the sell phase
     * @param user is the user to send that is finished
     */
    public void onFinishSellPhase(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        long finishedUser = 0;
        if (sellPhase) {
            marketHashMap.put(user, true);
            finishedUser = marketHashMap.entrySet().stream()
                    .filter(java.util.Map.Entry::getValue)
                    .count();
            long connectedUser = users.stream()
                    .filter(BaseUser::isConnected)
                    .count();
            if (finishedUser >= connectedUser) {
                sellPhase = false;
                marketHashMap.clear();
                startBuyPhase();
            } else {
                System.out.println("No : " + finishedUser + " " + connectedUser);
            }
        }
    }

    /** Send that buy phase is started
     */
    private void startBuyPhase() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        sendSnapshotToAll();
        buyPhase = true;
        selectRandomUser();
    }

    /** Select random user for buy phase in market
     */
    private void selectRandomUser() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Random random = new Random();
        int userNumber = 0;
        boolean found = false;
        while (!found) {
            userNumber = random.nextInt(users.size());
            if ((!marketHashMap.containsKey(users.get(userNumber)) || !marketHashMap.get(users.get(userNumber)))
                    && users.get(userNumber).isConnected() && !(users.get(userNumber) instanceof FakeUser)) {
                found = true;
            }
        }
        System.out.println("Sending start buy phase to " + users.get(userNumber));
        users.get(userNumber).getBaseCommunication().sendStartBuyPhase();
    }

    /** Send that buy phase is finished
     * @param user is the user where i have received that he has completed this phase
     */
    public void onFinishBuyPhase(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (buyPhase) {
            sendSnapshotToAll();
            marketHashMap.put(user, true);

            long finishedUser = marketHashMap.entrySet().stream()
                    .filter(java.util.Map.Entry::getValue)
                    .count();

            long connectedUser = users.stream()
                    .filter(BaseUser::isConnected)
                    .count();

            if (finishedUser < connectedUser) {
                selectRandomUser();
            } else {
                marketHashMap.clear();
                buyPhase = false;
                sendFinishMarketToAll();
                turnCounter = users.size();
                changeRound(nextUser);
            }
        }
    }

    /** Gets the bonus of the city where i have build
     * @param city1 is the city
     * @param user is the user that must win the bonus
     * @throws ActionNotPossibleException is the exception if action is not possible
     */
    public void getCityRewardBonus(City city1, User user) throws ActionNotPossibleException {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        City city = game.getCity(city1);
        if (checkBonusCorrect(city, user)) {
            try {
                if (!city.getColor().getColor().equals(Constants.PURPLE))
                    city.getBonus(user, game);
            } catch (ActionNotPossibleException e) {
            }
            user.decrementOptionalActionCounter();
            sendSnapshotToAll();
        } else {
            user.getBaseCommunication().selectCityRewardBonus(new SnapshotToSend(game, user));
            throw new ActionNotPossibleException(Constants.CITY_REWARD_BONUS_INCORRECT);
        }
    }

    /** Check if bonus is not a nobility bonus
     * @param city is the city with bonus chosen
     * @param user is the user that wants the bonus
     * @return true if it goes all well
     */
    private boolean checkBonusCorrect(City city, User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<Bonus> bonusArrayList = city.getBonus().getBonusArrayList();
        for (Bonus bonus : bonusArrayList) {
            if (bonus instanceof NobilityBonus) {
                return false;
            }
        }
        for (City city1 : user.getUsersEmporium()) {
            if (city1.equals(city)) {
                return true;
            }
        }
        return false;
    }

    /** Get PermitCard action where i buy it
     * @param permitCard is the permit bought
     * @param user is the user who has bought it
     */
    public void onSelectPermitCard(PermitCard permitCard, User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        PermitDeck permitDeck = game.getPermitDeck(permitCard.getRetroType());
        try {
            PermitCard permitCardTrue = permitDeck.getAndRemovePermitCardVisible(permitCard);
            permitCardTrue.getBonus().getBonus(user, game);
            user.addPermitCard(permitCardTrue);
            user.decrementOptionalActionCounter();
        } catch (ActionNotPossibleException e) {
            e.printStackTrace();
        }
        sendSnapshotToAll();
    }

    /** Change the Master User if is disconnected
     */
    public void changeMasterUser() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        // send map to first user
        for (User user : users) {
            if (user.isConnected()) {
                sendAvailableMap(user);
            }
        }
    }

    /** Start the last round because of the build of 10 emporia
     */
    public void startingLastRound() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        lastUser = nextUser % game.getUsers().size();
    }

    /** Check the user who has win
     */
    public void checkUserWhoWin() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        ArrayList<User> firstNobilityPathUserToReward = new ArrayList<>(users);
        ArrayList<User> secondNobilityPathUserToReward = new ArrayList<>(users);
        ArrayList<User> userMaxPermitCard = new ArrayList<>(users);
        ArrayList<User> userMaxHelper = new ArrayList<>(users);
        User userToRewardMaxPermitCard = new User();
        User userWithMaxHelperAndPoliticCard = new User();
        firstNobilityPathUserToReward.remove(fakeUser);
        secondNobilityPathUserToReward.remove(fakeUser);
        userMaxPermitCard.remove(fakeUser);
        userMaxHelper.remove(fakeUser);
        users.remove(fakeUser);
        sortingOnNobiliy(firstNobilityPathUserToReward);
        sortingOnNobiliy(secondNobilityPathUserToReward);
        sortingOnPermit(userMaxPermitCard);
        sortingOnHelper(userMaxHelper);
        for (Iterator<User> itr = firstNobilityPathUserToReward.iterator(); itr.hasNext(); ) {
            User userUsed = itr.next();
            if (firstNobilityPathUserToReward.get(0).getNobilityPathPosition().getPosition() > userUsed.getNobilityPathPosition().getPosition()) {
                itr.remove();
            }
        }
        secondNobilityPathUserToReward.removeAll(firstNobilityPathUserToReward);
        for (Iterator<User> itr = secondNobilityPathUserToReward.iterator(); itr.hasNext(); ) {
            User userUsed = itr.next();
            if (secondNobilityPathUserToReward.get(0).getNobilityPathPosition().getPosition() > userUsed.getNobilityPathPosition().getPosition()) {
                itr.remove();
            }
        }
        for (Iterator<User> itr = userMaxPermitCard.iterator(); itr.hasNext(); ) {
            User userUsed = itr.next();
            if (userMaxPermitCard.get(0).getPermitCards().size() > userUsed.getPermitCards().size()) {
                itr.remove();
            }
        }
        for (Iterator<User> itr = secondNobilityPathUserToReward.iterator(); itr.hasNext(); ) {
            User userUsed = itr.next();
            if (userMaxHelper.get(0).getHelpers().size() + userMaxHelper.get(0).getPoliticCardSize() > userUsed.getHelpers().size() + userUsed.getPoliticCardSize()) {
                itr.remove();
            }
        }
        firstNobilityPathUserToReward.forEach(user -> user.setVictoryPathPosition(user.getVictoryPathPosition() + 5));
        secondNobilityPathUserToReward.forEach(user -> user.setVictoryPathPosition(user.getVictoryPathPosition() + 2));
        userToRewardMaxPermitCard.setVictoryPathPosition(userToRewardMaxPermitCard.getVictoryPathPosition() + 3);
        ArrayList<User> userWhoWin = new ArrayList<>(users);
        userWhoWin = checkFirst(userWhoWin);
        if (userWhoWin.size() > 1) {
            for (User user : users) {
                if (user.equals(userWithMaxHelperAndPoliticCard)) {
                    user.setVictoryPathPosition(user.getVictoryPathPosition() + 3);
                }
            }
            userWhoWin = checkFirst(userWhoWin);
        }
        sortingOnWin(users);
        ArrayList<BaseUser> finalSnapshot = new ArrayList<>();
        for (User userInArray : users) {
            finalSnapshot.add(new BaseUser(userInArray));
        }
        for (User user : users) {
            user.getBaseCommunication().sendMatchFinishedWithWin(finalSnapshot);
        }
    }

    /** Sort the win array
     * @param arrayList sorted
     */
    private void sortingOnWin(ArrayList<User> arrayList) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Collections.sort(arrayList, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                if (o1.getVictoryPathPosition() > o2.getVictoryPathPosition())
                    return -1;
                if (o2.getVictoryPathPosition() > o1.getVictoryPathPosition())
                    return 1;
                else
                    return 0;
            }
        });
    }

    /** Sort the helper array
     * @param arrayList sorted
     */
    private void sortingOnHelper(ArrayList<User> arrayList) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Collections.sort(arrayList, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                if (o1.getHelpers().size() + o1.getPoliticCardSize() > o2.getHelpers().size() + o2.getPoliticCardSize())
                    return -1;
                if (o1.getHelpers().size() + o1.getPoliticCardSize() < o2.getHelpers().size() + o2.getPoliticCardSize())
                    return 1;
                else
                    return 0;
            }
        });
    }

    /** Sort the permit array
     * @param arrayList sorted
     */
    private void sortingOnPermit(ArrayList<User> arrayList) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Collections.sort(arrayList, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                if (o1.getPermitCards().size() > o2.getPermitCards().size())
                    return -1;
                if (o1.getPermitCards().size() < o2.getPermitCards().size())
                    return 1;
                else
                    return 0;
            }
        });
    }

    /** Sort the nobility array
     * @param arrayList sorted
     */
    private void sortingOnNobiliy(ArrayList<User> arrayList) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Collections.sort(arrayList, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                if (o1.getNobilityPathPosition().getPosition() > o2.getNobilityPathPosition().getPosition())
                    return -1;
                if (o1.getNobilityPathPosition().getPosition() < o2.getNobilityPathPosition().getPosition())
                    return 1;
                else
                    return 0;
            }
        });
    }

    /** Check the first user
     * @param arrayList is the array with ranking
     * @return the array of user sorted
     */
    private ArrayList<User> checkFirst(ArrayList<User> arrayList) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        Collections.sort(arrayList, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                if (o1.getVictoryPathPosition() > o2.getVictoryPathPosition())
                    return -1;
                if (o1.getVictoryPathPosition() < o2.getVictoryPathPosition())
                    return 1;
                else
                    return 0;
            }
        });
        for (User user : arrayList) {
            for (Iterator<User> itr = arrayList.iterator(); itr.hasNext(); ) {
                User userUsed = itr.next();
                if (arrayList.get(0).getVictoryPathPosition() > userUsed.getVictoryPathPosition()) {
                    itr.remove();
                }
            }
        }
        return arrayList;
    }

    /** Set the default things where user passes
     * @param user is the user who passes
     */
    public void onUserPass(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (users.indexOf(user) == (nextUser % users.size()) && !buyPhase && !sellPhase) {
            user.setMainActionCounter(0);
            user.setFastActionCounter(0);
            user.decrementOptionalActionCounter();
        }
    }

    /** Called when a user is disconnected
     * @param user is the user offline
     */
    public void onUserDisconnected(User user) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        users.forEach(user1 -> {
            if (user1.isConnected()) {
                user1.getBaseCommunication().sendUserDisconnect(user.getUsername());
            }
        });
        if (buyPhase) {
            onFinishBuyPhase(user);
        } else {
            if (sellPhase)
                onFinishSellPhase(user);
        }
    }

    /** Select the old permit card
     * @param user is the user who have choice the old permit card
     * @param permitCard is the permit card choosen
     */
    public void onSelectOldPermitCard(User user, PermitCard permitCard) {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        try {
            permitCard.getBonus().getBonus(user, game);
            user.decrementOptionalActionCounter();

        } catch (ActionNotPossibleException e) {
            e.printStackTrace();
        }
        sendSnapshotToAll();

    }

    /** Is the routine of the user initialization (set true is connected)
     * @return true if it goes well
     */
    public boolean userConnectedRoutine() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        for (User user : users) {
            if (user.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /** Clean the game because of the finish
     */
    public synchronized void cleanGame() {
        InternalLog.loggingSituation(this.getClass().getName(), new Object(){}.getClass().getEnclosingMethod().getName());
        if (!userConnectedRoutine()) {
            roundTimer.cancel();
            users.clear();
            GamesManager.getInstance().cancelThisGame(game, this);
        }
    }

    /** Equals
     * @param o is the object
     * @return true if it goes well
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameController that = (GameController) o;
        return fakeUser != null ? fakeUser.equals(that.fakeUser) : that.fakeUser == null;

    }

    /** HashCode
     * @return position
     */
    @Override
    public int hashCode() {
        return fakeUser != null ? fakeUser.hashCode() : 0;
    }
}
