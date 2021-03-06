#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
require 'fastercsv'
class SystemController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required

  def index
    @server=Server.new
    
    respond_to do |format|
      format.html
      format.csv  { 
        send_data(to_csv, :type => 'text/csv; charset=utf-8', :disposition => 'attachment; filename=sonar_system_info.csv')
      }
    end
  end
  
  private
  
  def to_csv
    FasterCSV.generate do |csv|
      @server.info.each do |property|
        csv << property
      end
    end
  end
end
