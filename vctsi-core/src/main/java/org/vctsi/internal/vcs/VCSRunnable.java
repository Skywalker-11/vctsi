package org.vctsi.internal.vcs;

/*-
 * #%L
 * vctsi-core
 * %%
 * Copyright (C) 2016 - 2017 Michael Pietsch (aka. Skywalker-11)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.sql.SQLException;
import java.util.Collection;

public abstract class VCSRunnable<MODULE extends VCSModule, ELEM> implements Runnable {
    protected MODULE module;
    private VCSThreadSpawner<MODULE, ELEM> spawner;

    public VCSRunnable() {
    }


    public void setSpawner(VCSThreadSpawner<MODULE, ELEM> spawner) {
        this.spawner = spawner;
    }

    /**
     * @param module this can be an object that will be reused on every run
     */
    void setModule(MODULE module) {
        this.module = module;
    }

    /**
     * this does the actual execution (import) of the elements
     *
     * @param elements the elements that shall be processed
     */
    public abstract void execute(Collection<ELEM> elements);


    /**
     * this will take elements from the queue provided by the threadspawner and executes them
     */
    @Override
    public void run() {
        try {
            if (module.dbSettings != null) {
                module.createThreadLocalSqlModule();
            }
        } catch (SQLException e) {
            module.addError(e.getMessage());
            Thread.currentThread().interrupt();
        }
        try {//to prevent the overhead for creating and destroying new threads and sql connection we keep this thread running
            while (true) {
                Collection<ELEM> elems = spawner.takeElem();
                if (elems == null || elems.isEmpty()) {
                    //no more data to process and we can stop
                    break;
                }
                execute(elems);
            }
            module.sqlModule.get().finishImport();
        } catch (InterruptedException e) {
            //do nothing
        } catch (SQLException e) {
            module.addError(e.getMessage());
        }
    }

}
