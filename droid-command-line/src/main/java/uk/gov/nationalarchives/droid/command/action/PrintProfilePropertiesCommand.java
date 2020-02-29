/**
 * Copyright (c) 2016, The National Archives <pronom@nationalarchives.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the The National Archives nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.droid.command.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

import org.apache.commons.configuration.ConfigurationException;

import uk.gov.nationalarchives.droid.profile.ProfileInstance;
import uk.gov.nationalarchives.droid.profile.ProfileManager;
import uk.gov.nationalarchives.droid.results.handlers.ProgressObserver;



/**
 * A command which prints the properties of one or more profiles to a PrintWriter.
 */
public class PrintProfilePropertiesCommand implements DroidCommand {

    private String[] profiles;
    private ProfileManager profileManager;
    private PrintWriter printWriter;

    /**
     * Empty bean style constructor.  Need to set profiles, profile manager and print writer before executing.
     */
    public PrintProfilePropertiesCommand() {
    }

    /**
     * Parameterized constructor.
     * @param profiles The profile ids
     * @param profileManager The profile manager.
     * @param printWriter The print writer to print with.
     */
    public PrintProfilePropertiesCommand(String[] profiles, ProfileManager profileManager, PrintWriter printWriter) {
        setProfiles(profiles);
        setProfileManager(profileManager);
        setPrintWriter(printWriter);
    }

    @Override
    public void execute() throws CommandExecutionException {
        for (String profileLocation : profiles) {
            ProfileInstance profile;
            try {
                profile = profileManager.open(Paths.get(profileLocation), new ProgressObserver() {
                    @Override
                    public void onProgress(Integer progress) {
                    }
                });
                printWriter.println();
                profile.getProperties().save(printWriter);
            } catch (IOException | ConfigurationException e) {
                throw new CommandExecutionException(e);
            }
        }
    }

    /**
     * Sets the profile ids to print.
     * @param profiles the profile ids to print.
     */
    public void setProfiles(String[] profiles) {
        this.profiles = profiles;
    }

    /**
     * Sets the print writer to print with.
     * @param printWriter the print writer to print with.
     */
    public void setPrintWriter(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    /**
     * Sets the profile manager used to obtain the profiles.
     * @param profileManager the profile manager used to obtain the profiles.
     */
    public void setProfileManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

}
