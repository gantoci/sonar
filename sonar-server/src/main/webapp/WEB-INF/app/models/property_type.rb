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
class PropertyType
  TYPE_INTEGER = 'INTEGER'
  TYPE_BOOLEAN = 'BOOLEAN'
  TYPE_FLOAT = 'FLOAT'
  TYPE_STRING = 'STRING'
  TYPE_METRIC = 'METRIC'
  TYPE_FILTER = 'FILTER'

  def self.text_to_value(text, type)
    case type
      when TYPE_INTEGER
        text.to_i
      when TYPE_FLOAT
        Float(text)
      when TYPE_BOOLEAN
        text=='true'
      when TYPE_METRIC
        Metric.by_key(text)
      else
        text
    end
  end

  def self.validate(key, type, optional, text_value, errors)
    errors.add_to_base("Unknown type for property #{key}") unless type
    if text_value.empty?
      errors.add_to_base("#{key} is empty") unless optional
    else
      errors.add_to_base("#{key} is not an integer") if type==PropertyType::TYPE_INTEGER && !Api::Utils.is_integer?(text_value)
      errors.add_to_base("#{key} is not a decimal number") if type==PropertyType::TYPE_FLOAT && !Api::Utils.is_number?(text_value)
      errors.add_to_base("#{key} is not a boolean") if type==PropertyType::TYPE_BOOLEAN && !(text_value=="true" || text_value=="false")
    end
  end
end