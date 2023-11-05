import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class SampQueryAPI {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int port;
    private boolean onlineStatus;

    public SampQueryAPI(String server, int port) throws UnknownHostException, SocketException {
        this.port = port;
        this.serverAddress = InetAddress.getByName(server);
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(2000); // Set timeout to 2 seconds
        this.onlineStatus = initializeConnection();
    }

    private boolean initializeConnection() {
        try {
            byte[] packet = createPacket("p4150".getBytes());
            sendPacket(packet);
            byte[] response = receivePacket();
            return new String(response).contains("p4150");
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isOnline() {
        return onlineStatus;
    }

    public ServerInfo getInfo() throws IOException {
        sendPacket(createPacket("i".getBytes()));
        byte[] response = receivePacket();
        return parseInfo(response);
    }

    public BasicPlayer[] getBasicPlayers() throws IOException {
        sendPacket(createPacket("c".getBytes()));
        byte[] response = receivePacket();
        return parseBasicPlayers(response);
    }

    public DetailedPlayer[] getDetailedPlayers() throws IOException {
        sendPacket(createPacket("d".getBytes()));
        byte[] response = receivePacket();
        return parseDetailedPlayers(response);
    }

    public Map<String, String> getRules() throws IOException {
        sendPacket(createPacket("r".getBytes()));
        byte[] response = receivePacket();
        return parseRules(response);
    }


    public static class ServerInfo {
        public final boolean password;

        public final int players;
        public final int maxPlayers;

        public final String hostname;
        public final String gamemode;
        public final String mapname;

        public ServerInfo(boolean password, int players, int maxPlayers, String hostname, String gamemode, String mapname) {
            this.password = password;
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.hostname = hostname;
            this.gamemode = gamemode;
            this.mapname = mapname;
        }

    }

    public static class BasicPlayer {
        public final String nickname;
        public final int score;

        public BasicPlayer(String nickname, int score) {
            this.nickname = nickname;
            this.score = score;
        }
    }

    public static class DetailedPlayer {
        public final int playerId;
        public final String nickname;
        public final int score;
        public final int ping;

        public DetailedPlayer(int playerId, String nickname, int score, int ping) {
            this.playerId = playerId;
            this.nickname = nickname;
            this.score = score;
            this.ping = ping;
        }
    }

    private byte[] createPacket(byte[] payload) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("SAMP".getBytes(), 0, 4);
        String[] parts = serverAddress.getHostAddress().split("\\.");
        for (String part : parts) {
            stream.write(Integer.parseInt(part));
        }
        stream.write(port & 0xFF);
        stream.write(port >> 8 & 0xFF);
        stream.write(payload, 0, payload.length);
        return stream.toByteArray();
    }

    private void sendPacket(byte[] packet) throws IOException {
        DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, serverAddress, port);
        socket.send(datagramPacket);
    }

    private byte[] receivePacket() throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return buffer;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private ServerInfo parseInfo(byte[] response) {
        ByteBuffer buffer = ByteBuffer.wrap(response);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the header part
        buffer.position(11);

        boolean password = buffer.get() != 0;
        int players = toUnsignedInt(buffer.getShort());
        int maxPlayers = toUnsignedInt(buffer.getShort());

        // This part is broken
        //int hostnameLength = toUnsignedInt(buffer.getShort());
        //byte[] hostnameBytes = new byte[hostnameLength];
        //buffer.get(hostnameBytes);
        //String hostname = new String(hostnameBytes);
        //
        //int gamemodeLength = toUnsignedInt(buffer.getShort());
        //byte[] gamemodeBytes = new byte[gamemodeLength];
        //buffer.get(gamemodeBytes);
        //String gamemode = new String(gamemodeBytes);
        //
        //int mapnameLength = toUnsignedInt(buffer.getShort());
        //byte[] mapnameBytes = new byte[mapnameLength];
        //buffer.get(mapnameBytes);
        //String mapname = new String(mapnameBytes);

        return new ServerInfo(password, players, maxPlayers, "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }

    private BasicPlayer[] parseBasicPlayers(byte[] response) {
        ByteBuffer buffer = ByteBuffer.wrap(response);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the header part
        buffer.position(11);

        int playerCount = toUnsignedInt(buffer.getShort());
        BasicPlayer[] basicPlayers = new BasicPlayer[playerCount];

        for (int i = 0; i < playerCount; i++) {
            int nameLength = toUnsignedInt(buffer.get());
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            String name = new String(nameBytes);
            int score = buffer.getInt();

            basicPlayers[i] = (new BasicPlayer(name, score));

        }

        return basicPlayers;
    }

    private DetailedPlayer[] parseDetailedPlayers(byte[] response) {
        ByteBuffer buffer = ByteBuffer.wrap(response);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the header part
        buffer.position(11);

        int playerCount = toUnsignedInt(buffer.getShort());
        DetailedPlayer[] detailedPlayers = new DetailedPlayer[playerCount];

        for (int i = 0; i < playerCount; i++) {
            int playerId = toUnsignedInt(buffer.get());

            int nameLength = toUnsignedInt(buffer.get());
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            String name = new String(nameBytes);

            int score = buffer.getInt();
            int ping = buffer.getInt();

            detailedPlayers[i] = (new DetailedPlayer(playerId, name, score, ping));

        }

        return detailedPlayers;
    }

    private Map<String, String> parseRules(byte[] response) {
        ByteBuffer buffer = ByteBuffer.wrap(response);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the header part
        buffer.position(11);

        int ruleCount = toUnsignedInt(buffer.getShort());
        Map<String, String> rules = new HashMap<>();

        for (int i = 0; i < ruleCount; i++) {
            int ruleNameLength = toUnsignedInt(buffer.get());
            byte[] ruleNameBytes = new byte[ruleNameLength];
            buffer.get(ruleNameBytes);
            String ruleName = new String(ruleNameBytes);

            int ruleValueLength = toUnsignedInt(buffer.get());
            byte[] ruleValueBytes = new byte[ruleValueLength];
            buffer.get(ruleValueBytes);
            String ruleValue = new String(ruleValueBytes);

            rules.put(ruleName, ruleValue);
        }

        return rules;
    }

    private int toUnsignedInt(short value) {
        return value & 0xFFFF;
    }

    private int toUnsignedInt(byte value) {
        return value & 0xFF;
    }

}
