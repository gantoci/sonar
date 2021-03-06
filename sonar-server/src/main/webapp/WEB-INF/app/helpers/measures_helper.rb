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
module MeasuresHelper

  def list_column_html(filter, column, widget_id)
    if column.sort?
      html = link_to_function(h(column.title_label), "refreshList#{widget_id}('#{escape_javascript column.key}',#{!filter.sort_asc?}, #{filter.criteria[:page]||1})", :title => h(column.tooltip))
    else
      html=h(column.title_label)
    end
    if column.period
      html += "<br><span class='note'>#{Api::Utils.period_abbreviation(column.period)}</small>"
    end
    if filter.sort_key==column.key
      html << (filter.sort_asc? ? image_tag("asc12.png") : image_tag("desc12.png"))
    end
    "<th class='#{column.align} #{column.title_css}'>#{html}</th>"
  end

  def list_cell_html(column, row)
    if column.metric
      measure = row.measure(column.metric)
      if column.period
        format_variation(measure, :index => column.period, :style => 'light')
      elsif column.metric.numeric?
        format_measure(measure) + trend_icon(measure, :empty => true)
      else
        format_measure(measure)
      end

    elsif column.key=='name'
      "#{qualifier_icon(row.snapshot)} #{link_to(row.snapshot.resource.name(true), {:controller => 'dashboard', :id => row.snapshot.resource_id}, :title => row.snapshot.resource.key)}"
    elsif column.key=='short_name'
      "#{qualifier_icon(row.snapshot)} #{link_to(row.snapshot.resource.name(false), {:controller => 'dashboard', :id => row.snapshot.resource_id}, :title => row.snapshot.resource.key)}"
    elsif column.key=='date'
      human_short_date(row.snapshot.created_at)
    elsif column.key=='key'
      "<span class='small'>#{row.snapshot.resource.kee}</span>"
    elsif column.key=='description'
      h row.snapshot.resource.description
    elsif column.key=='version'
      h row.snapshot.version
    elsif column.key=='language'
      Api::Utils.language_name(row.snapshot.resource.language)
    elsif column.key=='links' && row.links
      html = ''
      row.links.select { |link| link.href.start_with?('http') }.each do |link|
        html += link_to(image_tag(link.icon, :alt => link.name), link.href, :class => 'nolink', :popup => true) unless link.custom?
      end
      html
    end
  end

  def measure_filter_star(filter, is_favourite)
    if is_favourite
      style='fav'
      title=message('click_to_remove_from_favourites')
    else
      style='notfav'
      title=message('click_to_add_to_favourites')
    end

    "<a href='#' class='measure-filter-star #{style}' filter-id='#{filter.id}' title='#{title}'></a>"
  end

  CLOUD_MIN_SIZE_PERCENT=60.0
  CLOUD_MAX_SIZE_PERCENT=240.0

  def cloud_font_size(value, min_size_value, max_size_value)
    divisor=max_size_value - min_size_value
    size=CLOUD_MIN_SIZE_PERCENT
    if divisor!=0.0
      multiplier=(CLOUD_MAX_SIZE_PERCENT - CLOUD_MIN_SIZE_PERCENT)/divisor
      size=CLOUD_MIN_SIZE_PERCENT + ((max_size_value - (max_size_value-(value - min_size_value)))*multiplier)
    end
    size.to_i
  end

  def period_labels
    [Api::Utils.period_label(1), Api::Utils.period_label(2), Api::Utils.period_label(3)]
  end

end
