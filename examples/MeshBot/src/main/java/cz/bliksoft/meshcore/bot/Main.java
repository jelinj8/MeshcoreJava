package cz.bliksoft.meshcore.bot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		try {

			BotCompanion companion = new BotCompanion("BSMeshBot", "COM23", 115200);
			try {
				companion.awaitAvailable(2000);
				// companion.getConfig().setChannel("private group", "aaaaabbbbbcccccdddddeeeeefffff00");
			} catch (TimeoutException | InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("Bot running, press return to exit.");
			InputStreamReader rdr = new InputStreamReader(System.in);
			rdr.read();
			System.out.println("Bot finished.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}